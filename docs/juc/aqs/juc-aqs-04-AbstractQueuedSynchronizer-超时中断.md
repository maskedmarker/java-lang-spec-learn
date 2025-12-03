# juc-AQS-方法注释-AbstractQueuedSynchronizer



## acquire类

### tryAcquireNanos

```text
public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
        
    return tryAcquire(arg) ||
        doAcquireNanos(arg, nanosTimeout);
}
```

### doAcquireNanos

```text
private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (nanosTimeout <= 0L)
        return false;
        
    final long deadline = System.nanoTime() + nanosTimeout;
    // 线程节点入队同步队列
    final Node node = addWaiter(Node.EXCLUSIVE);
    
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            // 只有第一个节点线程才能去抢占,后面的节点线程需要等待
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null;
                failed = false;
                return true;
            }
            
            // 如果超时退出循环并返回false表示抢占锁失败
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L)
                return false;
                
            if (shouldParkAfterFailedAcquire(p, node) &&
                nanosTimeout > spinForTimeoutThreshold) // 如果需要等待的时间太短就通过for循环来完成自旋忙等待,此时通过park(timeout)来实现等待的cpu上下文切换开销占比太大.
                LockSupport.parkNanos(this, nanosTimeout);
            
            // 在park前中后都有可能发生中断
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    } finally {
        // 超时和中断都可能引起退出该方法.在退出前将线程节点从同步队列移除
        if (failed)
            cancelAcquire(node);
    }
}
```



### acquireInterruptibly(支持中断)

acquire方法的支持中断版本

```text
public final void acquireInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
        
    if (!tryAcquire(arg)) 
        doAcquireInterruptibly(arg); // 尝试失败则通过等待队列完成
}
```

### doAcquireInterruptibly(支持中断)

acquireQueued方法的支持中断版本.
直接抛出中断异常而非以返回值告知调用方

```text
private void doAcquireInterruptibly(int arg) throws InterruptedException {
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null;
                failed = false;
                return;
            }
            if (shouldParkAfterFailedAcquire(p, node) && // 如果当前节点的前节点标记好了SIGNAL后才能挂起当前线程
                parkAndCheckInterrupt()) 
                throw new InterruptedException(); // 如果当前线程是因为中断才退出park的话,直接抛出中断异常
        }
    } finally {
        if (failed)
            cancelAcquire(node); // 中断导致结束等待
    }
}
```



## release类

release操作不应该被设计成会发生阻塞等待,所以不用关心中断和超时.

