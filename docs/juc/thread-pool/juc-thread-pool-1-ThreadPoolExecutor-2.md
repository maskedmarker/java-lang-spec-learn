# juc-ThreadPoolExecutor

## 核心方法

### 实现AbstractExecutorService的execute方法

AbstractExecutorService的execute方法在ThreadPoolExecutor的语义是,由其他线程来执行提交的任务

Params:
command – the task to execute


```text
向线程池提交任务的处理逻辑:
优先创建核心线程来立即执行用户提交的任务  ->  其次暂时缓存到工作队列中,稍后执行  -> 再次工作队列满了后,创建非和核心线程来执行用户提交的任务  -> 线程数已达最大值,且工作队列满了,按决绝策略处理用户提交的任务

也就是说: 优先使用队列,而不是创建非核心线程💯💯💯

这样的策略并不是普世性的.💯💯💯
tomcat的线程池面对新增连接的I/O操作,在允许创建的最大线程数内,优先创建线程,而不是优先排队.
因为任务是耗时的I/O操作,创建线程的时间成本就没有那么大.

总结:
是选择使用队列还是选择创建非核心线程? 主要取决于任务执行时间与创建线程的时间成本谁大.
```

```text
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();

    int c = ctl.get();
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true))                        // 如果工作线程数还未达到核心线程数的最大值,新增工作线程且优先执行任务(addWorker中也会判断当前线程池的运行状态)
            return;
        c = ctl.get();                                       // 新增工作线程失败时,ctl有变化,所以再次读取
    }
    
    // 代码运行到这里,创建核心线程失败.原因是 1.工作线程已经达到核心线程数最大值 或者2.线程池进入了SHUTDOWN阶段
    if (isRunning(c) && workQueue.offer(command)) {          // 如果线程池的运行状态正常, 且工作线程已经达到核心线程数最大值,则优先将任务放到待处理任务队列中
        int recheck = ctl.get();
        if ((!isRunning(recheck)) && remove(command))        // 先向workQueue添加新增任务,就必须二次检查runState(remove失败表示已经被其他工作线程领走了或者关闭线程池时被清空了,所以不用处理这个场景)
            reject(command);                                 // 线程池处于非正常状态则移除已提交的任务,按决绝策略处理新提交的任务(抛异常提醒/调用者的线程来执行/悄悄抛弃)
        
        else if (workerCountOf(recheck) == 0)                // 如果线程池中已经没有任何工作线程了,但任务已经成功进入工作队列,则必须创建一个新的工作线程保证会执行工作队列中的任务💯💯💯
            addWorker(null, false);                          // 注意addWorker(null, false)中的command为null,表明是为了处理无人执行的任务而创建的收尾线程
    }
    
    // 代码运行到这里,原因是 1.线程池进入了SHUTDOWN阶段,不能新增工作线程 或者2.workQueue满了 
    else if (!addWorker(command, false))                      // 如果无法创建非收尾线程,证明是工作队列满了,按决绝策略处理新提交的任务
        reject(command);
}

上面多处使用了if-elseif,可以按照switch的方式来理解
if (isRunning(c) && workQueue.offer(command)) 当已经达到核心线程数了,且此时线程池的运行状态正常,优先将任务放到待处理任务队列中
    if ((!isRunning(recheck)) && remove(command)) 当任务放到待处理任务队列中后,线程池的运行状态不正常,则将刚提交的任务从队列中撤回,并拒绝该任务
    else if (workerCountOf(recheck) == 0) 当任务放到待处理任务队列中后,线程池的的工作线程数为0,则新增所谓非核心线程

```

```text
设计动机:
    优先保持核心线程活跃：避免频繁创建/销毁线程带来的开销,核心线程是基本的长期工作者.
    使用队列减少线程创建：若任务瞬时到达很多优先使用任务队列缓冲,线程池不盲目扩到maximumPoolSize,从而节省资源.
    当任务队列扩容到maximumPoolSize,只有在队列容纳不下,才扩容.这样可以在高并发突发负载时扩容,但不会因为短时峰值而一直保持大量线程.
拒绝策略：最后保留处理,防止无限制内存/线程消耗.
```


### 创建工作线程 addWorker

为线程池创建工作线程,并启动线程(调用start方法)去执行任务

```text
private boolean addWorker(Runnable firstTask, boolean core) {

    // 这个2层for循环主要目的是通过CAS来完成合适的runState下更新workerCount
    retry:
    for (;;) { // 2层for循环,外层是用来读取最新的runState值,内存用来增加ctl-workerCount值
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
            
            if (compareAndIncrementWorkerCount(c))             // 通过break-retry标签确保成功更新ctl-workerCount(即先更新workerCount再后续实际创建线程)
                break retry; 
            // 代码运行到此处意味着前面的CAS-ctl-workerCount失败.需要重新读取最新的ctl的值
            c = ctl.get();
            // 如果仅仅是workerCount变化而runState没变化,就继续执行内循环,否则重新执行外循环
            if (runStateOf(c) != rs)
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }

    // 前面已经成功增加ctl-workerCount值,现在要真正创建工作线程
    
    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        w = new Worker(firstTask);                                                             // Worker的ThreadFactory会为其创建Thread对象(Thread并未触发start)
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            // 对于修改workers,必须使用mainLock.(shutdown的执行涉及到修改workers也要mainLock,所以addWorker和shutdown不会并发执行)
            // 由于线程池的状态变化由shutdown引起,由于mainLock的存在导致在addWorker时无法执行shutdown和execute,也就在这里锁定了线程池的状态
            mainLock.lock();
            try {
                // 再次检查线程池状态,防止从CAS更新workerCount的值到现在这段时间发生了shutdown
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {                   // 线程池处于正常阶段允许增工作线程,或者SHUTDOWN阶段补充工作线程尽快完成剩余积压任务
                    if (t.isAlive())                                                            // 防止ThreadFactory().newThread()返回的thread是已经被start
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
                t.start();                                                                       // 由线程池来start工作线程线程
                workerStarted = true;                                                            // workers.add(w)成功后再start工作线程
            }
        }
    } finally { // try-finally是为了防止Worker创建线程失败
        if (! workerStarted)
            addWorkerFailed(w);                                                                  // 如果start失败,从workers中移除新增worker,并且减少ctl-workerCount值,且tryTerminate
    }
    return workerStarted;
}
```

```text
创建新的工作线程(即新创建线程ThreadPoolExecutor.Worker)
受线程数约束:
    当core=true时,是否要创建新的worker受corePoolSize限制,如果未达到核心线程数的最大值则创建,否则返回false提示创建工作线程失败.
    当core=false时,是否要创建新的worker受maximumPoolSize限制,如果未达到最大线程数的最大值则创建,否则返回false提示创建工作线程失败.
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

## Tomcat 的线程池策略

```text
Tomcat使用的是自己的一套实现：
org.apache.tomcat.util.threads.ThreadPoolExecutor + org.apache.tomcat.util.threads.TaskQueue

Tomcat 的核心目标是: 优先创建线程,而不是优先排队
1 创建核心线程
2 如果线程数 < maxThreads → 继续创建线程
3 只有达到 maxThreads 才开始排队
4 队列满 → 拒绝

Tomcat的核心技巧：在不改变ThreadPoolExecutor.execute()的逻辑的逻辑下,通过修改TaskQueue.offer()行为,从而让ThreadPoolExecutor.execute()优先创建非核心线程-次优使用工作队列.
```

```text
org.apache.tomcat.util.net.AbstractEndpoint#createExecutor

public void createExecutor() {
    internalExecutor = true;
    TaskQueue taskqueue = new TaskQueue();
    TaskThreadFactory tf = new TaskThreadFactory(getName() + "-exec-", daemon, getThreadPriority());
    executor = new ThreadPoolExecutor(getMinSpareThreads(), getMaxThreads(), 60, TimeUnit.SECONDS, taskqueue, tf);     // org.apache.tomcat.util.threads.ThreadPoolExecutor使用定制化的org.apache.tomcat.util.threads.TaskQueue
    taskqueue.setParent( (ThreadPoolExecutor) executor);                                                               // 为TaskQueue设置相关的org.apache.tomcat.util.threads.ThreadPoolExecutor
}
```

```text
public class org.apache.tomcat.util.threads.ThreadPoolExecutor extends java.util.concurrent.ThreadPoolExecutor {
    
    // jdk的ThreadPoolExecutor预留的回调入口
    protected void afterExecute(Runnable r, Throwable t) {
        submittedCount.decrementAndGet();                         // submittedCount统计“已提交但尚未完成”的任务数量(in-flight tasks)   activeCount依赖mainLock,高并发下读取成本高

        if (t == null) {
            stopCurrentThreadIfNeeded();
        }
    }
    
    
    public void execute(Runnable command) {
        execute(command,0,TimeUnit.MILLISECONDS);
    }
    
    public void execute(Runnable command, long timeout, TimeUnit unit) {
        submittedCount.incrementAndGet();
        
        try {
            super.execute(command);
        } catch (RejectedExecutionException rx) {
            if (super.getQueue() instanceof TaskQueue) {
                final TaskQueue queue = (TaskQueue)super.getQueue();
                try {
                    if (!queue.force(command, timeout, unit)) {
                        submittedCount.decrementAndGet();
                        throw new RejectedExecutionException(sm.getString("threadPoolExecutor.queueFull"));
                    }
                } catch (InterruptedException x) {
                    submittedCount.decrementAndGet();
                    throw new RejectedExecutionException(x);
                }
            } else {
                submittedCount.decrementAndGet();
                throw rx;
            }

        }
    }    
}
```

```text
public class TaskQueue extends LinkedBlockingQueue<Runnable> {

    // 使用该TaskQueue的org.apache.tomcat.util.threads.ThreadPoolExecutor
    private transient volatile ThreadPoolExecutor parent = null;

    public boolean offer(Runnable o) {
        // ...
        
        // 如果线程池达到最大线程数的话,使用工作队列缓存新增任务
        if (parent.getPoolSize() == parent.getMaximumPoolSize()) return super.offer(o);
        
        // (当前工作线程数还未达到最大,且有空闲线程) 如果当前线程池正在处理的任务数据还没有工作线程数多,将任务放入工作队列,立马会有空闲工作线程来执行,无需创建线程
        if (parent.getSubmittedCount()<=(parent.getPoolSize())) return super.offer(o);
        
        // (当前工作线程数还未达到最大,且无空闲工作线程), 通过强制返回false让线程池创建更多非核心工作线程💯💯💯
        if (parent.getPoolSize()<parent.getMaximumPoolSize()) return false;
        
        // 其他场景再无创建工作线程可能,只能使用工作队列缓存新增任务
        return super.offer(o);
    }
}

```

## 动态调整线程池大小

ThreadPoolExecutor支持动态调整线程池的核心参数
```text
public void setMaximumPoolSize(int maximumPoolSize) { //...}
public void setCorePoolSize(int corePoolSize) { //...}
```