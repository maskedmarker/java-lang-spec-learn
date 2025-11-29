# juc-AQS-ReentrantLock

## Lock
```text
public interface Lock {
    void lock();
    boolean tryLock();
    
    void lockInterruptibly() throws InterruptedException;
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    
    void unlock();
    Condition newCondition();
}
```

```text
public Condition newCondition() {
    return sync.newCondition();
}
```


### tryRelease(独占模式)(不支持中断)

因为当前时独占模式,即只有持有锁的那一个线程才会调用,不存在并发,所以tryRelease的实现

```text
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    // 因为是独占模式,需要检查当前线程否是是锁的持有者
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null); // 如果是重入锁,持有3个permit,现在release 2个,还剩一个,此时就不能将set null
    }
    
    // 因为是独占模式方法,不存在并发,只需要一个write可见就行
    setState(c);
    return free;
}
```

### tryReleaseShared(共享模式)(不支持中断)

因为当前时共享模式,会出现多个线程同时调用,所以tryReleaseShared通过循环规避并发时的CAS操作失败

```text
protected final boolean tryReleaseShared(int releases) {
    // 存在并发CAS操作,CAS操作失败后通过不断重试来达到成功
    for (;;) {
        int current = getState();
        int next = current + releases;
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        
        // 这里使用CAS来设置state,因为是共享模式方法,存在并发
        if (compareAndSetState(current, next))
            return true;
    }
}
```