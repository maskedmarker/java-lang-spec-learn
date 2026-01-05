# juc-forkjoin-ForkJoinPool



## 向ForkJoinPool线程池提交任务

### invoke/execute


```text
// 提交任务并等待任务执行完毕(同步执行)
public <T> T invoke(ForkJoinTask<T> task) {
    if (task == null)
        throw new NullPointerException();
    
    // 向线程池提交任务
    externalPush(task);
    
    return task.join(); // 等待执行完成
}

// 提交任务但不等待任务执行完毕(异步执行)
public void execute(ForkJoinTask<?> task) {
    if (task == null)
        throw new NullPointerException();
        
    externalPush(task); // 提交任务后,不用等待任务执行完成
}
```

### 其他变种

入参Runnable/Callable需要封装到ForkJoinTask

```text
// 异步执行
public void execute(Runnable task) {
    if (task == null)
        throw new NullPointerException();
    ForkJoinTask<?> job;
    if (task instanceof ForkJoinTask<?>) // avoid re-wrap
        job = (ForkJoinTask<?>) task;
    else
        job = new ForkJoinTask.RunnableExecuteAction(task);
    externalPush(job);
}

// 异步执行
public <T> ForkJoinTask<T> submit(Callable<T> task) {
    ForkJoinTask<T> job = new ForkJoinTask.AdaptedCallable<T>(task);
    externalPush(job);
    return job;
}
```



## 工具方法

### lockRunState

通过CAS设置runState的最低位来锁定ForkJoinPool的运行状态.
锁定失败的话,等待.

只有lockRunState/unlockRunState能修改runState,其他方法只能读runState;lockRunState设置锁bit,unlockRunState清除锁bit.
且lockRunState/unlockRunState成对使用.

如下方法会使用到lockRunState
tryAddWorker registerWorker deregisterWorker tryCompensate tryTerminate externalSubmit

```text
private int lockRunState() {
    int rs;
    
    // 一定要先判定lock-bit位是否没有加锁,然后才能cas(old-value, old-value |= RSLOCK),否则就等于是抢了其他线程已经持有的锁,违反了锁的不可抢占性.
    return ((((rs = runState) & RSLOCK) != 0 ||
             !U.compareAndSwapInt(this, RUNSTATE, rs, rs |= RSLOCK)) ?
            awaitRunStateLock() : rs);
}


------------------------------------------------------------------------------------------
Doug Lea 常用的写法, 等级于
private int lockRunState() {
    int rs = runState;
    
    if ((rs & RSLOCK) == 0){
        if(U.compareAndSwapInt(this, RUNSTATE, rs, rs |= RSLOCK)) {
            return rs;
        }
    }
    
    // 锁已经被持有,就需要等待
    return awaitRunStateLock();
}
```

### unlockRunState

lockRunState/unlockRunState的设计意图是,先使用cas实现持有锁＋cas自旋等待,降低使用monitor锁的概率.

lockRunState/unlockRunState成对使用的范式如下
```text
int rs = lockRunState();  // 返回加锁前的runState,runState的0th-bit有设置(即locked),但是1st-bit没有设置是未知的(可能携带signal,也可能没有)
exe_code_in_critical_section;
unlockRunState(rs, rs & ~RSLOCK);
```


怎么挂起要看怎么唤醒.

只有lockRunState/unlockRunState能修改runState,其他方法只能读runState.
且lockRunState/unlockRunState成对使用.

```text
private void unlockRunState(int oldRunState, int newRunState) {
    if (!U.compareAndSwapInt(this, RUNSTATE, oldRunState, newRunState)) {  // cas操作失败只可能是oldRunState缺少signal位,最新的runState被设置了signal,.(如果没有发生wait,就不会触发cpu开销比较大的notify操作)
        Object lock = stealCounter;
        runState = newRunState;              // cas操作失败证明oldRunState有lock位缺少signal位, newRunState=(oldRunState & ~RSLOCK)即而和lock位,所以此时newRunState可以成为runState的新值
        if (lock != null)
            synchronized (lock) { lock.notifyAll(); }  // runState被设置了signal,证明有线程需要唤醒  (因为fork-join的任务可以被steal,使用notifyAll而非notify是为了让更多工作线程恢复调度,能更充分利用cpu的并行能力)
    }
}
```


#### awaitRunStateLock

Spins and/or blocks until runstate lock is available. See above for explanation.
awaitRunStateLock是lockRunState的专属方法,仅仅是为了保持逻辑清晰才被提取到一个独立方法中,不会被其他方法使用

```text
// 返回值为CAS-runState时的原值
private int awaitRunStateLock() {
    Object lock;
    boolean wasInterrupted = false;
    
    // 先自旋等待(支持自旋模式),然后挂起等待(这就是所谓的adaptive自适应锁)
    for (int spins = SPINS, r = 0, rs, ns;;) {
        // 只在每个循环的开始处读取一个runState
        if (((rs = runState) & RSLOCK) == 0) {
            if (U.compareAndSwapInt(this, RUNSTATE, rs, ns = rs | RSLOCK)) {  // 只有当lock-bit位显示锁已经释放了,才能cas尝试抢锁
                if (wasInterrupted) {
                    try {
                        Thread.currentThread().interrupt();
                    } catch (SecurityException ignore) {
                    }
                }
                return ns; // rs未携带lock位,那么rs要么读取到初始值(未携带signal位),要么读取到首次unlockRunState设置的值(也未携带signal),如此后续ns会保持一直未携带signal
            }
        }
        
        // 当前和下个else-if都用于生成随机数r(没有使用),同时生成随机数消耗了cpu时间,达到了自旋等待
        else if (r == 0)                               
            r = ThreadLocalRandom.nextSecondarySeed();
        else if (spins > 0) {
            r ^= r << 6; r ^= r >>> 21; r ^= r << 7;  // 伪随机数生成器中的核心运算
            if (r >= 0)
                --spins;
        }
        
        // 因为awaitRunStateLock方法依赖stealCounter的monitor,所以要等待ForkJoinPool初始完成   (外部线程首次提交任务时触发初始化过程((runState & RSLOCK) != 0)以及分配stealCounter)
        else if ((rs & STARTED) == 0 || (lock = stealCounter) == null)      
            Thread.yield();
        
        // ForkJoinPool初始已完成,且已经被其他线程占有了ForkJoinPool的锁
        else if (U.compareAndSwapInt(this, RUNSTATE, rs, rs | RSIGNAL)) {   // 多个lockRunState可以并发执行,但通过循环重试后依次完成cas
            synchronized (lock) {                                           
                if ((runState & RSIGNAL) != 0) {                            // 从cas-runState-RSIGNAL到获取lock的monitor之间有时间间隙,这个时间间隙内runState会发生变化(比如unlockRunState抢先一步执行了)
                    try {
                        lock.wait();                                        // 自旋等待后,锁还没有释放,就通过monitor机制挂起等待. 
                    } catch (InterruptedException ie) {
                        if (!(Thread.currentThread() instanceof ForkJoinWorkerThread))
                            wasInterrupted = true;
                    }
                }
                else
                    lock.notifyAll();   // else可以删除,因为有lock的monitor锁的互斥性不会有遗漏notify的情况.
            }
        }
    }
}

lockRunState/unlockRunState成对使用.
一个unlockRunState会唤醒所有抢锁的线程,但是一个线程能成功cas成功,其他的线程又会因为抢锁失败进入wait等待.

synchronized (lock)代码块没有使用while-wait,因为synchronized (lock)代码块外层有一个类似与while(true)的for循环.同时此处不用while可以实现wait+自旋混合式的等待.
```

### tryCompensate

Tries to decrement active count (sometimes implicitly) and possibly release or create a compensating worker in preparation for blocking. 

Params: w – caller
Returns false (retryable by caller), on contention, detected staleness, instability, or termination.

在某个内部工作线程即将发生阻塞时(非空闲挂起,因为业务原因),用极端谨慎的多层校验来维持线程池并行度稳定.
        判断ForkJoinPool是否需要/以及是否能够,通过“激活空闲线程/减少活跃计数/创建新worker”来补偿线程池的并行度.
返回: true → 当前worker可以安全阻塞; false → 不允许阻塞(继续自旋或重试)

```text
private boolean tryCompensate(WorkQueue w) {
    boolean canBlock;
    WorkQueue[] ws; long c; int m, pc, sp;
    
    if (w == null || w.qlock < 0 ||                                    // caller terminating
        (ws = workQueues) == null || (m = ws.length - 1) <= 0 ||       // caller terminating
        (pc = config & SMASK) == 0)                                    // parallelism disabled 并行度被禁用,不允许任何补偿
        canBlock = false;
    else if ((sp = (int)(c = ctl)) != 0)                        // (当前线程将要阻塞)如果有空闲的工作线程,尝试去激活它,保持线程池线程的并行度
        canBlock = tryRelease(c, ws[sp & m], 0L);
    else {                                                      // 没有空闲的工作线程,新增工作线程
        int ac = (int)(c >> AC_SHIFT) + pc;
        int tc = (short)(c >> TC_SHIFT) + pc;
        int nbusy = 0;                                           // 统计忙碌的worker数,验证“是否真的满载”
        for (int i = 0; i <= m; ++i) {                           // two passes of odd indices
            WorkQueue v;
            if ((v = ws[((i << 1) | 1) & m]) != null) {
                if ((v.scanState & SCANNING) != 0)               //工作线程在执行任务,而正在找任务
                    break;
                ++nbusy;
            }
        }
        
        // 📌线程池状态状态不稳定,返回false让调用方重试
        if (nbusy != (tc << 1) || ctl != c)                      // 因为遍历2遍奇数索引,所以nbusy最大可以是total-count的2倍
            canBlock = false;                                    // unstable or stale   
        
        // 📌已达到最大并行度,当前工作线程没有待处理的任务,可以安心挂起,不会影响线程池的处理任务的并行度   ((nbusy == (tc << 1)):其他工作线程都在忙)
        else if (tc >= pc && ac > 1 && w.isEmpty()) {                         // (tc >= pc)已达到并行度, (ac > 1)为了兜底, (w.isEmpty())当前工作线程的工作队列没有任务了  
            long nc = ((AC_MASK & (c - AC_UNIT)) | (~AC_MASK & c));           // active-count减一 
            canBlock = U.compareAndSwapLong(this, CTL, c, nc);
        }
        
        else if (tc >= MAX_CAP || (this == common && tc >= pc + commonMaxSpares))
            throw new RejectedExecutionException("Thread limit exceeded replacing blocked worker");
        
        // 📌当前还未达到最大并行度,通过新增工作线程,保证线程池并行度不降(当前线程即将阻塞)
        else {                                // similar to tryAddWorker
            boolean add = false; int rs;      // CAS within lock
            long nc = ((AC_MASK & c) | (TC_MASK & (c + TC_UNIT)));  // total-count加一,active-count不变
            
            if (((rs = lockRunState()) & STOP) == 0)
                add = U.compareAndSwapLong(this, CTL, c, nc);
            unlockRunState(rs, rs & ~RSLOCK);
            canBlock = add && createWorker(); // throws on exception
        }
    }
    return canBlock;
}
```

### managedBlock

```text
public static void managedBlock(ManagedBlocker blocker)
    throws InterruptedException {
    ForkJoinPool p;
    ForkJoinWorkerThread wt;
    Thread t = Thread.currentThread();
    
    if ((t instanceof ForkJoinWorkerThread) && (p = (wt = (ForkJoinWorkerThread)t).pool) != null) {    // 当前线程是内部内部工作线程
        WorkQueue w = wt.workQueue;
        
        while (!blocker.isReleasable()) {                    // 当blocker不满足解除阻塞的条件时
            if (p.tryCompensate(w)) {
                try {
                    do {} while (!blocker.isReleasable() &&
                                 !blocker.block());
                } finally {
                    U.getAndAddLong(p, CTL, AC_UNIT);
                }
                break;
            }
        }
    } else {                                                                                              // 当前线程是外部线程
        do {} while (!blocker.isReleasable() &&
                     !blocker.block());
    }
}
```