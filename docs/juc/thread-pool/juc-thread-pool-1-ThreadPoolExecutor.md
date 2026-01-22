# juc-ThreadPoolExecutor


```text
没有所谓的核心线程和非核心线程,只有核心线程数.
线程池的工作线程数在达到corePoolSize之前的所有线程称为所谓的核心线程,工作线程数在达到corePoolSize之后,多余的线程在逻辑上划分为所谓的非核心线程.工作线程在创建之后并不会被贴一个所谓的核心或非核心线程的标签.
```

## ThreadPoolExecutor

workQueue由用户提供.

```text
public class ThreadPoolExecutor extends AbstractExecutorService {
    // 线程池的初始状态就是running
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    // The queue used for holding tasks and handing off to worker threads. (保存待执行的任务,后续会由工作线程来处理)
    private final BlockingQueue<Runnable> workQueue;
    // workers用mainLock来保护同一时刻只有一下线程修改;workQueue自带锁机制线程安全的(BlockingQueue is thread-safe)
    private final HashSet<Worker> workers = new HashSet<Worker>();
    private final ReentrantLock mainLock = new ReentrantLock();
    
    private volatile int maximumPoolSize;
    private volatile int corePoolSize;
    
    // 如下2个属性是用来统计该线程池的指标的
    private int largestPoolSize; // 跟踪线程池曾经达到的最大线程数
    private long completedTaskCount; // 在线程池的工作线程终止时,将工作线程对象中的completedTasks合并到线程池中
    
   // 实现了Executor的核心方法
   void execute(Runnable command){...}
   // AbstractExecutorService已经提前实现好了submit之类的方法
   Future<?> submit(Runnable task){...}
}

// 线程池的工作线程被封装成了Worker类(并没有核心线程和非核心线程这样的概念,有核心线程数(注意词汇是数)这样的概念,核心线程数用来控制线程池中空闲线程的收缩)
private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
    final Thread thread;
    Runnable firstTask;
    // 统计该线程已经完成的任务数
    volatile long completedTasks;
    
    Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker (state的初始值为-1, 用途:在runWorker之前禁止中断)
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }
    
    public void run() {
        // 线程池start工作线程后,新的工作线程会调用Worker.run方法,继而执行ThreadPoolExecutor.runWorker(worker)方法,同一个线程池中的各个工作线程的不同点是Worker对象,所以runWorker的入参需要Worker对象.
        runWorker(this);
    }
    public void lock()        { acquire(1); }
    public boolean tryLock()  { return tryAcquire(1); }
    public void unlock()      { release(1); }
    protected boolean isHeldExclusively() {
        return getState() != 0;
    }
}
```

## 线程池的状态

```text
private static final int RUNNING    = -1 << COUNT_BITS;   // 111_00000000000000000000000000000
private static final int SHUTDOWN   =  0 << COUNT_BITS;   // 000_00000000000000000000000000000
private static final int STOP       =  1 << COUNT_BITS;   // 001_00000000000000000000000000000
private static final int TIDYING    =  2 << COUNT_BITS;   // 010_00000000000000000000000000000
private static final int TERMINATED =  3 << COUNT_BITS;   // 011_00000000000000000000000000000

The main pool control state, ctl, is an atomic integer packing two conceptual fields 
workerCount, indicating the effective number of threads 
runState, indicating whether running, shutting down etc 
In order to pack them into one int, we limit workerCount to (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2 billion) otherwise representable. 

高3个bit用来表示线程池的运行状态(runState)
低29个bit用来表示工作线程数(workerCount)

The workerCount is the number of workers that have been permitted to start and not permitted to stop. 
The value may be transiently different(与实际有片刻的不同) from the actual number of live threads, for example when a ThreadFactory fails to create a thread when asked, and when exiting threads are still performing bookkeeping before terminating. 
----------------------------------------

The runState provides the main lifecycle control, taking on values: 
RUNNING:    Accept new tasks and process queued tasks (存在新增任务和新增工作线程)
SHUTDOWN:   Don't accept new tasks, but process queued tasks (不存在新增任务,存在为了处理挤压的任务而新增工作线程)
STOP:       Don't accept new tasks, don't process queued tasks, and interrupt in-progress tasks (不存在新增任务也不存在新增工作线程,因为不再处理任务也就无需新增工作线程)
TIDYING:    All tasks have terminated, workerCount is zero, the thread transitioning to state TIDYING will run the terminated() hook method (不存在新增任务也不存在新增工作线程)
TERMINATED: terminated() has completed (不存在新增任务也不存在新增工作线程)

The numerical order among these values matters, to allow ordered comparisons. (这些状态被设置了有序的大小,方便比较)
The runState monotonically(单调地/单向地) increases over time, but need not hit each state. (线程池的运行状态是单向的)


The transitions are: 
RUNNING -> SHUTDOWN On invocation of shutdown(), perhaps implicitly in finalize() 
(RUNNING or SHUTDOWN) -> STOP On invocation of shutdownNow() 
SHUTDOWN -> TIDYING When both queue and pool are empty 
STOP -> TIDYING When pool is empty 
TIDYING -> TERMINATED When the terminated() hook method has completed Threads waiting in awaitTermination() will return when the state reaches TERMINATED. 

Detecting the transition from SHUTDOWN to TIDYING is less straightforward than you'd like because the queue may become empty after non-empty and vice versa during SHUTDOWN state, but we can only terminate if, after seeing that it is empty, we see that workerCount is 0 (which sometimes entails a recheck -- see below).
```


## 核心方法

### 创建工作线程 addWorker

为线程池创建工作线程, 然后由线程池启动线程(调用start方法)


```text
创建新的工作线程(即新创建线程ThreadPoolExecutor.Worker)
受线程数约束:
    当core=true时,是否要创建新的worker受corePoolSize限制,如果未达到核心线程数的最大值,则创建.
    当core=false时,是否要创建新的worker受maximumPoolSize限制,如果未达到最大线程数的最大值,则创建.
受线程池状态约束:
    如果线程池已经进入SHUTDOWN流程中时(rs==SHUTDOWN)不再接收任务,但可以接受新增工作线程来处理积压的任务(此时要求firstTask==null && !workQueue.isEmpty())
    如果线程池已经进入STOP+流程中时(rs>=STOP)不再新增工作线程

参数core仅仅用来表明调用方希望新建的线程后的线程数是否超过核心线程数


Checks if a new worker can be added with respect to current pool state and the given bound (either core or maximum). 
If so, the worker count is adjusted accordingly, and, if possible, a new worker is created and started, running firstTask as its first task. 
This method returns false if the pool is stopped or eligible to shut down. ()
It also returns false if the thread factory fails to create a thread when asked. If the thread creation fails, either due to the thread factory returning null, or due to an exception (typically OutOfMemoryError in Thread.start()), we roll back cleanly.
Params:
firstTask – the task the new thread should run first (or null if none). 
            Workers are created with an initial first task (in method execute()) to bypass queuing when there are fewer than corePoolSize threads (in which case we always start one), or when the queue is full (in which case we must bypass queue). 
            Initially idle threads are usually created via prestartCoreThread or to replace other dying workers.
core – if true use corePoolSize as bound, else maximumPoolSize. (A boolean indicator is used here rather than a value to ensure reads of fresh values after checking other pool state)

--------------------------------------------------------------------------

由于线程池状态是单向变更的,可以不通过加锁来排除一定不能创建工作线程的情况.比如
if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))
            return false;
            
 如果是check and update的场景,采用乐观锁的概念先CAS+再二次确定+循环重试.比如
if (compareAndIncrementWorkerCount(c)) 
    break retry;
if (runStateOf(c) != rs)
    continue retry;
--------------------------------------------------------------------------

private boolean addWorker(Runnable firstTask, boolean core) {

    // 这个2层for循环主要目的是通过CAS来完成合适的runState下更新workerCount
    retry:
    for (;;) { // 2层for循环,外层是用来读取最新的runState值
        int c = ctl.get();
        int rs = runStateOf(c);

        // 如果线程池已经进入SHUTDOWN流程中时(rs==SHUTDOWN)不再接收任务,但可以接受新增工作线程来处理积压的任务(此时要求firstTask==null && !workQueue.isEmpty())
        // 如果线程池已经进入STOP+流程中时(rs>=STOP)不再新增工作线程
        if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))
            return false;

        for (;;) { // 2层for循环,内层是用来CAS更新workerCount的值
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||                              // 如果当前线程数已经达到jdk限制的最大值,则创建工作线程失败
                wc >= (core ? corePoolSize : maximumPoolSize)) // 如果当前线程数已经达到用户设定的最大线程数,则创建工作线程失败
                return false;
            if (compareAndIncrementWorkerCount(c)) // 更新ctl值中的workerCount成功,就跳出循环(即先更新workerCount再后续实际创建线程)
                break retry;
            
            // 代码运行到此处意味着前面的CAS失败.需要重新读取最新的ctl的值
            c = ctl.get();
            // 如果仅仅是workerCount变化而runState没变化,就继续执行内循环,否则重新执行外循环
            if (runStateOf(c) != rs)
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }

    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        w = new Worker(firstTask); // Worker的ThreadFactory会为其创建Thread对象(Thread并未触发start)
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            // 对于修改workers,必须使用mainLock.(shutdown的执行涉及到修改workers也要mainLock,所以addWorker和shutdown不会并发执行)
            // 由于线程池的状态变化由shutdown引起,由于mainLock的存在导致在addWorker时无法执行shutdown和execute,也就在这里锁定了线程池的状态
            mainLock.lock();
            try {
                // 再次检查线程池状态,防止从CAS更新workerCount的值到现在这段时间发生了shutdown
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) { // 当线程池处于正常工作状态或者处于SHUTDOWN时且仅增工作线程,可以允许增工作线程
                    if (t.isAlive()) // 防止Worker的thread已经被调用了start
                        throw new IllegalThreadStateException();
                    workers.add(w);
                    int s = workers.size();
                    if (s > largestPoolSize) // 跟踪线程池曾经达到的最大线程数
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            if (workerAdded) {
                // 由线程池来start工作线程线程,新的工作线程会调用Worker.run方法
                t.start();
                workerStarted = true; // workers.add(w)成功后再start工作线程
            }
        }
    } finally { // try-finally是为了防止Worker创建线程失败
        if (! workerStarted)
            addWorkerFailed(w); // 如果start失败,会将之前added工作线程从workers中剔除
    }
    return workerStarted;
}
```

### 提前创建核心工作线程 prestartCoreThread

```text
类似于缓存预热,可以提前创建核心线程(而非等到任务到来时再创建)

public boolean prestartCoreThread() {
    return workerCountOf(ctl.get()) < corePoolSize &&
        addWorker(null, true);
}
```

### getTask

```text
从任务队列中获取任务,且如果返回null表示工作线程可以正常结束了.
对于所谓核心线程可以一直等待到获取到新的任务(有开关设置可以不用无穷等待),
对于所谓非核心线程只等待(空闲)一段时间后还没有任务就要结束工作线程了(即此时返回null)



Performs blocking or timed wait for a task, depending on current configuration settings, or returns null if this worker must exit because of any of: 
1. There are more than maximumPoolSize workers (due to a call to setMaximumPoolSize). 
2. The pool is stopped. 
3. The pool is shutdown and the queue is empty. 
4. This worker timed out waiting for a task, and timed-out workers are subject to termination (that is, allowCoreThreadTimeOut || workerCount > corePoolSize) both before and after the timed wait, and if the queue is non-empty, this worker is not the last thread in the pool.
Returns:
task, or null if the worker must exit, in which case workerCount is decremented (返回null时,workerCount已经自动减1了)

private Runnable getTask() {
    boolean timedOut = false; // Did the last poll() time out?

    for (;;) {
        int c = ctl.get(); //第一次读取线程池状态
        int rs = runStateOf(c);

        // 如果此时线程池的状态是STOP,不会再有新任务了;如果状态是SHUTDOWN且工作队列也空了,不会再有待处理的工作了.此时可以放心结束获取任务的工作线程.
        // 如果状态是SHUTDOWN且工作队列未空,可能还需要核心和非核心工作线程继续处理队列中的任务
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }

        int wc = workerCountOf(c);

        // 对于所谓的非核心线程,只等待有限的时间(先忽略allowCoreThreadTimeOut开关)
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
        
        // wc > maximumPoolSize针对的是maximumPoolSize在线程池运行中被重新设置了一个较小的值,此时主动收缩工作线程数
        // (timed && timedOut) 当前工作线程数过多,且在上个循环中已经(空闲)等待了足够的时间,可以结束了
        // (wc > 1 || workQueue.isEmpty()) 同时如果任务队列不为空还需要所谓的非核心线程;如果工作线程数都都只剩1个了也不能在收缩工作线程了,即至少要保持1个线程来继续工作
        if ((wc > maximumPoolSize || (timed && timedOut))
            && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c)) // CAS操作确保第一次读取线程池状态没有发生变化,防止多个工作线程同时执行
                return null;
            continue; // 第一次读取线程池状态发生变化了,通过循环再次尝试
        }

        try {
            // poll和take使用都是Lock.lockInterruptibly(),在等待新增任务到来前被阻塞时,如果被其他线程中断了会结束阻塞并抛出中断异常(此时中断状态位被清除)
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                workQueue.take();
            if (r != null)
                return r;
            // 如果没有抛出异常也就意味着poll和take正常超时退出等待    
            timedOut = true;
        } catch (InterruptedException retry) {
            // 即使poll和take抛出中断异常(可能是shutdown触发的,也可能是任务触发的)也会被吞掉,但是会标记非超时退出
            timedOut = false;
        }
    }
}

当线程池处于正常状态下,工作线程在getTask()中被workQueue.take()阻塞时,此时该线程被其他线程中断,getTask()会吞掉中断标识位,然后在后面的循环中获取到了新的任务(如果是shutdown则无法获取到新任务).整个过程就像是没有发生中断.
interruptIdleWorkers()仅仅中断所有的工作线程但并不改变线程池状态
shutdown会触发interruptIdleWorkers()和改变线程池状态,导致getTask()返回null;
setCorePoolSize重新设置线程池的线程数大小,也可以触发interruptIdleWorkers(),但并不改变线程池状态,此时getTask()可以返回非null,且吞掉中断标识位
```

### 实现AbstractExecutorService的execute方法

AbstractExecutorService的execute方法在ThreadPoolExecutor的语义是,由其他线程来执行提交的任务

Params:
command – the task to execute



```text

设计动机:
    优先保持核心线程活跃：避免频繁创建/销毁线程带来的开销，核心线程是基本的长期工作者。
    使用队列减少线程创建：若任务瞬时到达很多优先使用任务队列缓冲，线程池不盲目扩到maximumPoolSize，从而节省资源。
    当任务队列扩容到maximumPoolSize,只有在队列容纳不下，才扩容。这样可以在高并发突发负载时扩容，但不会因为短时峰值而一直保持大量线程。
拒绝策略：最后保留处理，防止无限制内存/线程消耗。

public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    /*
     * Proceed in 3 steps:
     *
     * 1. If fewer than corePoolSize threads are running, try to start a new thread with the given command as its first task.
     *  The call to addWorker atomically checks runState and workerCount, and so prevents false alarms that would add threads when it shouldn't, by returning false.
     *
     * 2. If a task can be successfully queued, then we still need to double-check whether we should have added a thread (because existing ones died since last checking) or that
     * the pool shut down since entry into this method. So we recheck state and if necessary roll back the enqueuing if stopped, or start a new thread if there are none.
     *
     * 3. If we cannot queue task, then we try to add a new thread.  If it fails, we know we are shut down or saturated and so reject the task.
     */
    int c = ctl.get();
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true)) // 如果线程数还未达到核心线程数的最大值,新增工作线程时以use corePoolSize as bound(同时addWorker中也会判断当前线程池的运行状态)
            return;                  // 创建工作线程成功,且工作线程以command作为firstTask来处理.
        c = ctl.get(); // 新增线程失败时,ctl有变化,所以再次读取
    }
    if (isRunning(c) && workQueue.offer(command)) { // 如果已经达到核心线程数了,且此时线程池的运行状态正常,优先将任务放到待处理任务队列中
        int recheck = ctl.get();
        if ((!isRunning(recheck)) && remove(command)) // 首次检查runState后在没有锁的情况下向工作队列中添加任务,所以必须需要二次检查runState,也算是类似乐观锁的实现;remove失败表示已经被其他工作线程领走了或者关闭线程池时被清空了,所以不用处理这个场景
            reject(command); // 一旦发现线程池处于关闭流程中,及时将刚提交的任务从队列中撤回,并拒绝该任务
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    else if (!addWorker(command, false)) // 如果任务队列满了,线程池的运行状态正常,新增线程(大于核心线程数)来完成任务
        reject(command); // 新增工作线程失败则拒绝任务
}

上面多处使用了if-elseif,可以按照switch的方式来理解
if (isRunning(c) && workQueue.offer(command)) 当已经达到核心线程数了,且此时线程池的运行状态正常,优先将任务放到待处理任务队列中
    if ((!isRunning(recheck)) && remove(command)) 当任务放到待处理任务队列中后,线程池的运行状态不正常,则将刚提交的任务从队列中撤回,并拒绝该任务
    else if (workerCountOf(recheck) == 0) 当任务放到待处理任务队列中后,线程池的的工作线程数为0,则新增所谓非核心线程

```


#### processWorkerExit

```text

Params:
w – the worker 
completedAbruptly – if the worker died due to user exception (为true时,工作线程在执行任务时,任务抛出了异常导致工作线程结束)

private void processWorkerExit(Worker w, boolean completedAbruptly) {
    if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
        decrementWorkerCount();

    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        completedTaskCount += w.completedTasks;
        workers.remove(w);
    } finally {
        mainLock.unlock();
    }

    tryTerminate();

    int c = ctl.get();
    if (runStateLessThan(c, STOP)) {
        if (!completedAbruptly) {
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
            if (min == 0 && ! workQueue.isEmpty())
                min = 1;
            if (workerCountOf(c) >= min)
                return; // replacement not needed
        }
        addWorker(null, false);
    }
}
```

#### tryTerminate

````text

Transitions to TERMINATED state if either (SHUTDOWN and pool and queue empty) or (STOP and pool empty). 
If otherwise eligible to terminate but workerCount is nonzero, interrupts an idle worker to ensure that shutdown signals propagate. 
This method must be called following any action that might make termination possible -- reducing worker count or removing tasks from the queue during shutdown. (tryTerminate由可能满足结束线程池的动作来间接触发,这些动作包括减少工作线程/较少任务)
The method is non-private to allow access from ScheduledThreadPoolExecutor.


final void tryTerminate() {
    for (;;) {
        int c = ctl.get();
        if (isRunning(c) ||
            runStateAtLeast(c, TIDYING) ||
            (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
            return;
            
        // 如果还有工作线程,就中断一个工作线程,然后提前退出该方法    
        if (workerCountOf(c) != 0) { // Eligible to terminate
            interruptIdleWorkers(ONLY_ONE);
            return;
        }
        
        // 此时工作线程数为零
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // TIDYING这个瞬时状态是为terminated()准备的
            if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                try {
                    terminated();
                } finally {
                    // 设置终态并触发Condition
                    ctl.set(ctlOf(TERMINATED, 0));
                    termination.signalAll();
                }
                return;
            }
        } finally {
            mainLock.unlock();
        }
        // else retry on failed CAS
    }
}

// 空方法,子类可以扩展
protected void terminated() { }
````



```text
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 先更新状态再中断工作线程,这里有时间间隙
        advanceRunState(SHUTDOWN);
        // 中断工作线程,希望工作线程不要再执行任务
        interruptIdleWorkers();
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
}


public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 先更新状态再中断工作线程,这里有时间间隙
        advanceRunState(STOP);
        // 中断工作线程,希望工作线程不要再执行任务
        interruptWorkers();
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
    return tasks;
}
```


### Worker的核心方法

#### Worker 

````text
因为单个Worker对象只会被一个线程执行,所以Worker只需要实现AbstractQueuedSynchronizer的独占模式的抽象方法

private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
    final Thread thread;
    Runnable firstTask;
    // 统计该线程已经完成的任务数
    volatile long completedTasks;
    
    Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker (state的初始值为-1, tryAcquire会一直失败, 除非先调用unlock()触发tryRelease才能将state设置为0)
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }
    
    public void run() {
        // 线程池start工作线程后,新的工作线程会调用Worker.run方法,继而执行ThreadPoolExecutor.runWorker(worker)方法,同一个线程池中的各个工作线程的不同点是Worker对象,所以runWorker的入参需要Worker对象.
        runWorker(this);
    }
    
    // 实现独占模式的tryAcquire方法,无需实现tryAcquireShared方法
    protected boolean tryAcquire(int unused) {
        // 通过CAS修改state来实现原子性(state的初始值为-1,这里CAS不会成功)
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }

    // 实现独占模式的tryRelease方法,无需实现tryReleaseShared方法
    protected boolean tryRelease(int unused) {
        setExclusiveOwnerThread(null);
        // 因为是独占模式,当前方法不会并发执行,所以连CAS都省了
        setState(0);
        return true;
    }
    
    public void lock()        { acquire(1); }
    public boolean tryLock()  { return tryAcquire(1); }
    public void unlock()      { release(1); }
    protected boolean isHeldExclusively() {
        return getState() != 0;
    }
}
````


#### runWorker

工作线程在开始执行新的任务前,需要合理的处理当前的中断位.
    如果线程池正常,此时工作线程在执行上个任务时被任务本身中断了,此时在开始新任务前需要清空这些中断位
    如果线程池处于停止中,在执行新任务前需要将自身设置成中断,以此来告诉将要执行的任务(线程池要停止了,任务需要合理地处理该情况)

```text
final void runWorker(Worker w) {
    // runWorker方法只有工作线程才会调用,所以currentThread就是当前的工作线程
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask; // 取出当前将要执行的任务
    w.firstTask = null; // 下次将要执行的任务还未知,所以设置为null
    
    // Worker的构造函数中将state设置为-1
    w.unlock(); // allow interrupts
    boolean completedAbruptly = true;
    try {
        while (task != null || (task = getTask()) != null) { // 如果Worker的firstTask为空,就需要从线程池的任务队列中获取
            // 加锁防止多个线程使用同一个worker(使用的是锁的不可中断模式)
            w.lock();
            
            // 线程池停止时通过中断工作线程来通知他们,这里通过线程池的工作状态来及时修复这些中断,防止这些中断位被其他因素被清除; (runStateAtLeast(ctl.get(), STOP) && !wt.isInterrupted())
            // 如果线程池正常,则需要确保工作线程的中断位被被清除(中断位是上个任务留下的,不清除则新任务可能看到这个中断信号而异常结束任务). ((Thread.interrupted() && runStateAtLeast(ctl.get(), STOP)) && !wt.isInterrupted())这种写法保证了线程池正常时工作线程的中断位被被清除
            // If pool is stopping, ensure thread is interrupted; if not, ensure thread is not interrupted.  This requires a recheck in second case to deal with shutdownNow race while clearing interrupt
            if ((runStateAtLeast(ctl.get(), STOP) || 
                 (Thread.interrupted() &&
                  runStateAtLeast(ctl.get(), STOP))) &&
                !wt.isInterrupted())
                wt.interrupt();
            try {
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x; // 任务如果抛出异常会导致工作线程提前结束
                } catch (Error x) {
                    thrown = x; throw x; // 任务如果抛出异常会导致工作线程提前结束
                } catch (Throwable x) {
                    thrown = x; throw new Error(x); // 任务如果抛出异常会导致工作线程提前结束
                } finally {
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                w.unlock();
            }
        }
        
        // 如果firstTask和工作队列都没有任务了,自动推出while的无穷循环
        completedAbruptly = false;
    } finally {
        processWorkerExit(w, completedAbruptly);
    }
}
```