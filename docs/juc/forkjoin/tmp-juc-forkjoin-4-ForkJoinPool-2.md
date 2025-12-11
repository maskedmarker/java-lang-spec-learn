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

如下方法会使用到lockRunState
tryAddWorker
registerWorker
deregisterWorker
tryCompensate
tryTerminate
externalSubmit

```text
private int lockRunState() {
    int rs;
    
    // 因为要判断某个bit位,所以无法无脑cas(specified-value, new-value),只能先if再cas
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
    return awaitRunStateLock();
}
```


### awaitRunStateLock

Spins and/or blocks until runstate lock is available. See above for explanation.

```text
// 返回值为CAS-runState时的原值
private int awaitRunStateLock() {
    Object lock;
    boolean wasInterrupted = false;
    for (int spins = SPINS, r = 0, rs, ns;;) {
        // 只有当((runState & RSLOCK) == 0)时才cas尝试抢占锁 (因为判断的某个bit位,所以无法无脑cas(specified-value, new-value))
        if (((rs = runState) & RSLOCK) == 0) {
            if (U.compareAndSwapInt(this, RUNSTATE, rs, ns = rs | RSLOCK)) {
                if (wasInterrupted) {
                    try {
                        Thread.currentThread().interrupt();
                    } catch (SecurityException ignore) {
                    }
                }
                return ns;
            }
        }
        
//        else if (r == 0)                               // 当前和下个else-if都是为了生成随机数r,结果随机数r并没有被使用(r应该是预留的)
//            r = ThreadLocalRandom.nextSecondarySeed();
//        else if (spins > 0) {
//            r ^= r << 6; r ^= r >>> 21; r ^= r << 7;  // 伪随机数生成器中的核心运算
//            if (r >= 0)
//                --spins;
//        }
        
        // 因为awaitRunStateLock方法依赖stealCounter的monitor,所以要等待ForkJoinPool初始完成   (外部线程首次提交任务时触发初始化过程((runState & RSLOCK) != 0)以及分配stealCounter)
        else if ((rs & STARTED) == 0 || (lock = stealCounter) == null)      
            Thread.yield();
        
        // ForkJoinPool初始已完成,且已经被其他线程占有了ForkJoinPool的锁
        else if (U.compareAndSwapInt(this, RUNSTATE, rs, rs | RSIGNAL)) {   // 竞争锁失败后,在runState上设置需要被唤醒的标识 (多个lockRunState可以并发执行,但通过循环重试后依次完成cas)
            synchronized (lock) {                                           // (stealCounter被当作runState的monitor锁)
                if ((runState & RSIGNAL) != 0) {                            // 再次检查runState
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        if (!(Thread.currentThread() instanceof ForkJoinWorkerThread))
                            wasInterrupted = true;
                    }
                }
                else
                    lock.notifyAll();
            }
        }
    }
}
```

```text
private void unlockRunState(int oldRunState, int newRunState) {
    if (!U.compareAndSwapInt(this, RUNSTATE, oldRunState, newRunState)) {
        Object lock = stealCounter;
        runState = newRunState;              // clears RSIGNAL bit
        if (lock != null)
            synchronized (lock) { lock.notifyAll(); }
    }
}
```