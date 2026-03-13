# juc-ThreadPoolExecutor

ExecutorService的主要实现可以分为ForkJoinPool和ThreadPoolExecutor这2个分支,以及一个扩展分支ScheduledExecutorService.

```text
没有所谓的核心线程和非核心线程,只有核心线程数.
线程池的工作线程数在达到corePoolSize之前的所有线程称为所谓的核心线程,工作线程数在达到corePoolSize之后,再新增的线程在逻辑上划分为所谓的非核心线程.工作线程在创建之后并不会被贴一个所谓的核心或非核心线程的标签.
```

## ThreadPoolExecutor

```text
public class ThreadPoolExecutor extends AbstractExecutorService {
    // 线程池的初始状态就是running
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    
    // The queue used for holding tasks and handing off to worker threads. (保存待执行的任务,后续会由工作线程来处理) 💯workQueue必须由用户提供,没有默认值
    private final BlockingQueue<Runnable> workQueue;
    
    // workers用mainLock来保护同一时刻只有一下线程修改;workQueue自带锁机制线程安全的(BlockingQueue is thread-safe)
    private final HashSet<Worker> workers = new HashSet<Worker>();
    private final ReentrantLock mainLock = new ReentrantLock();
    
    // 非final,在线程池运行中可以调整核心线程数和最大线程数💯
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
```

```text
// 线程池的工作线程被封装成了Worker类(并没有核心线程和非核心线程这样的概念,有核心线程数(注意词汇是数)这样的概念,核心线程数用来控制线程池中空闲线程的收缩)
// Worker直接继承AQS,而不是将AQS作为内部类使用,主要是省去了重新定义lock/unlock之类的接口
private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
    final Thread thread;  // 替worker执行任务的线程
    Runnable firstTask;   // 线程跑起来后执行的第一个任务(一般是用户刚提交的任务),完成后再从workQueue中主动拿
    
    // 统计该线程已经完成的任务数
    volatile long completedTasks;
    
    Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker (state的初始值为-1, 用途:在runWorker之前禁止中断)
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }
    
    public void run() {
        // 线程池start后,执行ThreadPoolExecutor.runWorker(worker)方法,同一个线程池中的各个工作线程的不同点是Worker对象,所以runWorker的入参需要Worker对象.
        runWorker(this);
    }
    
    // 加锁类的接口
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
高3个bit用来表示线程池的运行状态(runState) 低29个bit用来表示工作线程数(workerCount)

private static final int COUNT_BITS = Integer.SIZE - 3;   // 29(-1/0/1/2/3这5个枚举值至少需要占用3个bit)
private static final int RUNNING    = -1 << COUNT_BITS;   // 111_00000000000000000000000000000
private static final int SHUTDOWN   =  0 << COUNT_BITS;   // 000_00000000000000000000000000000
private static final int STOP       =  1 << COUNT_BITS;   // 001_00000000000000000000000000000
private static final int TIDYING    =  2 << COUNT_BITS;   // 010_00000000000000000000000000000
private static final int TERMINATED =  3 << COUNT_BITS;   // 011_00000000000000000000000000000

The main pool control state, ctl, is an atomic integer packing two conceptual fields 
workerCount, indicating the effective number of threads 
runState, indicating whether running, shutting down etc 
In order to pack them into one int, we limit workerCount to (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2 billion) otherwise representable. 


The workerCount is the number of workers that have been permitted to start and not permitted to stop. 
The value may be transiently different(与实际有片刻的不同) from the actual number of live threads, for example when a ThreadFactory fails to create a thread when asked, and when exiting threads are still performing bookkeeping before terminating. 
----------------------------------------------------------------------------------------------------------------------------------------------------------------

The runState provides the main lifecycle control, taking on values: 
RUNNING:    Accept new tasks and process queued tasks (存在新增任务和新增工作线程)
SHUTDOWN:   Don't accept new tasks, but process queued tasks (不存在新增任务,存在为了处理挤压的任务而新增工作线程)
STOP:       Don't accept new tasks, don't process queued tasks, and interrupt in-progress tasks (不存在新增任务也不存在新增工作线程,因为不再处理任务也就无需新增工作线程)
TIDYING:    All tasks have terminated, workerCount is zero, the thread transitioning to state TIDYING will run the terminated() hook method (不存在新增任务也不存在新增工作线程)
TERMINATED: terminated() has completed (不存在新增任务也不存在新增工作线程)

The numerical order among these values matters, to allow ordered comparisons. (这些状态被设置了有序的大小,方便比较)
The runState monotonically(单调地/单向地) increases over time, but need not hit each state. (线程池的运行状态是单向转变化,但也不是每个状态节点都要经过💯💯💯)


The transitions are: 
RUNNING -> SHUTDOWN On invocation of shutdown(), perhaps implicitly in finalize() 
(RUNNING or SHUTDOWN) -> STOP On invocation of shutdownNow() 
SHUTDOWN -> TIDYING When both queue and pool are empty 
STOP -> TIDYING When pool is empty 
TIDYING -> TERMINATED When the terminated() hook method has completed Threads waiting in awaitTermination() will return when the state reaches TERMINATED. 

Detecting the transition from SHUTDOWN to TIDYING is less straightforward than you'd like because the queue may become empty after non-empty and vice versa during SHUTDOWN state, but we can only terminate if, after seeing that it is empty, we see that workerCount is 0 (which sometimes entails a recheck -- see below).
```

## 备忘

```text
// Invokes shutdown when this executor is no longer referenced and it has no threads.
protected void finalize() {
    shutdown();
}
```