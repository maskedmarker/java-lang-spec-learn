# juc-ScheduledThreadPoolExecutor

```text
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {

    // Sequence number to break scheduling ties(平局), and in turn to guarantee FIFO order among tied entries.
    private static final AtomicLong sequencer = new AtomicLong();
    
    
    // False if should cancel/suppress periodic tasks on shutdown.
    private volatile boolean continueExistingPeriodicTasksAfterShutdown;

    // False if should cancel non-periodic tasks on shutdown.
    private volatile boolean executeExistingDelayedTasksAfterShutdown = true;

    // True if ScheduledFutureTask.cancel should remove from queue
    private volatile boolean removeOnCancel = false;
    
    public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS, new DelayedWorkQueue());    // 使用自己的内部类DelayedWorkQueue
    }
    
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (callable == null || unit == null)
            throw new NullPointerException();
        RunnableScheduledFuture<V> t = decorateTask(callable, new ScheduledFutureTask<V>(callable, triggerTime(delay, unit)));
        delayedExecute(t);
        return t;
    }
    
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        if (period <= 0)
            throw new IllegalArgumentException();
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<Void>(command, null, triggerTime(initialDelay, unit), unit.toNanos(period));
        RunnableScheduledFuture<Void> t = decorateTask(command, sft);
        sft.outerTask = t;
        delayedExecute(t);
        return t;
    }
    
    // Throws: RejectedExecutionException – if the task cannot be scheduled for execution
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        if (delay <= 0)
            throw new IllegalArgumentException();
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<Void>(command, null, triggerTime(initialDelay, unit), unit.toNanos(-delay));
        RunnableScheduledFuture<Void> t = decorateTask(command, sft);
        sft.outerTask = t;
        delayedExecute(t);
        return t;
    }
    
    
    private void delayedExecute(RunnableScheduledFuture<?> task) {
        if (isShutdown())
            reject(task);
        else {
            super.getQueue().add(task);
            if (isShutdown() &&
                !canRunInCurrentRunState(task.isPeriodic()) &&
                remove(task))
                task.cancel(false);
            else
                ensurePrestart();
        }
    }
}
```

```text
private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {

    private final long sequenceNumber;
    
    // 下次执行的时间
    private long time;
    private final long period;
    
    // The actual task to be re-enqueued by reExecutePeriodic
    RunnableScheduledFuture<V> outerTask = this;
    
    // Index into delay queue, to support faster cancellation.
    int heapIndex;
    
    public void run() {
        boolean periodic = isPeriodic();
        if (!canRunInCurrentRunState(periodic))                  // 判断线程池是否正在关闭
            cancel(false);
        else if (!periodic)
            ScheduledFutureTask.super.run();                     // 非周期性任务,执行完后,不再放入线程池的任务队列.
        else if (ScheduledFutureTask.super.runAndReset()) {
            setNextRunTime();
            reExecutePeriodic(outerTask);                        // 如果是周期性任务,本次正常执行完成后,重新设置下次触发时间,并将当前任务重新放入线程池的任务队列中,等待下次执行  (周期性任务如果执行时发生异常,就不再周期性重复执行了💯💯💯)
        }
    }    
    
    private void setNextRunTime() {
        long p = period;
        if (p > 0)
            time += p;                   // period为正数表示不考虑执行时间,就是周期性的
        else
            time = triggerTime(-p);      // period为负值表示执行完后的时间+往后延迟一段时间
    }
    
    public int compareTo(Delayed other) {
        if (other == this) // compare zero if same object
            return 0;
        
        if (other instanceof ScheduledFutureTask) {
            ScheduledFutureTask<?> x = (ScheduledFutureTask<?>)other;
            long diff = time - x.time;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else if (sequenceNumber < x.sequenceNumber)    // 延迟时间相同的话,使用序列号排序
                return -1;
            else
                return 1;
        }
        long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
        return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
    }
}
```

```text
static class DelayedWorkQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {
    private static final int INITIAL_CAPACITY = 16;
    
    // A DelayedWorkQueue is based on a heap-based data structure like those in DelayQueue and PriorityQueue, except that every ScheduledFutureTask also records its index into the heap array
    private RunnableScheduledFuture<?>[] queue = new RunnableScheduledFuture<?>[INITIAL_CAPACITY];
    
    private int size = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();
    
    // Thread designated to wait for the task at the head of the queue. 
    // This variant of the Leader-Follower pattern (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to minimize unnecessary timed waiting. 
    // When a thread becomes the leader, it waits only for the next delay to elapse, but other threads await indefinitely. 
    // The leader thread must signal some other thread before returning from take() or poll(...), unless some other thread becomes leader in the interim. 
    // Whenever the head of the queue is replaced with a task with an earlier expiration time, the leader field is invalidated by being reset to null, and some waiting thread, but not necessarily the current leader, is signalled. 
    // So waiting threads must be prepared to acquire and lose leadership while waiting.
    private Thread leader = null;
    
    
    
    // 内部使用了小顶堆
    public boolean offer(Runnable x) {
        if (x == null) throw new NullPointerException();
        
        RunnableScheduledFuture<?> e = (RunnableScheduledFuture<?>)x;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = size;
            if (i >= queue.length)
                grow();
            size = i + 1;
            
            if (i == 0) {
                queue[0] = e;
                setIndex(e, 0);
            } else {
                siftUp(i, e);
            }
            
            if (queue[0] == e) {
                leader = null;
                available.signal();
            }
        } finally {
            lock.unlock();
        }
        return true;
    }
    
    
    public RunnableScheduledFuture<?> poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            RunnableScheduledFuture<?> first = queue[0];
            if (first == null || first.getDelay(NANOSECONDS) > 0)
                return null;
            else
                return finishPoll(first);
        } finally {
            lock.unlock();
        }
    }   
    
    // ThreadPoolExecutor线程池在执行任务时,会调用take方法从工作队列获取任务💯💯💯
    // 小顶堆,从堆顶获取的元素就是下个触发时间最近的任务
    // 如果当前还未到达触发时间,阻塞线程
    public RunnableScheduledFuture<?> take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                RunnableScheduledFuture<?> first = queue[0];
                if (first == null)
                    available.await();
                else {
                    long delay = first.getDelay(NANOSECONDS);
                    if (delay <= 0)
                        return finishPoll(first);
                    
                    first = null;                // don't retain ref while waiting
                    if (leader != null)
                        available.await();
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            available.awaitNanos(delay);
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && queue[0] != null)
                available.signal();
            lock.unlock();
        }
    }
    
    public RunnableScheduledFuture<?> poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                RunnableScheduledFuture<?> first = queue[0];
                if (first == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay = first.getDelay(NANOSECONDS);
                    if (delay <= 0)
                        return finishPoll(first);
                    if (nanos <= 0)
                        return null;
                    
                    first = null; // don't retain ref while waiting
                    if (nanos < delay || leader != null)
                        nanos = available.awaitNanos(nanos);
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && queue[0] != null)
                available.signal();
            lock.unlock();
        }
    }      
}
```