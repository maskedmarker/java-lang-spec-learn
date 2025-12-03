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


## await类

### awaitNanos

```text
public final long awaitNanos(long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    
     
    // We don't check for nanosTimeout <= 0L here, to allow awaitNanos(0) as a way to "yield the lock".
    final long deadline = System.nanoTime() + nanosTimeout;
    long initialNanos = nanosTimeout;
    
    // 当前线程已经获取了锁,同步队列中的线程节点已经移除;现在向条件队列中新增条件节点
    Node node = addConditionWaiter();
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    
    // 等待:其他线程在释放锁时,通过signal方法将本线程节点从条件队列转移到同步队列
    while (!isOnSyncQueue(node)) {
        if (nanosTimeout <= 0L) {
            // 如果发生超时,主动将线程节点从条件队列转移到同步队列
            transferAfterCancelledWait(node);
            break;
        }
        
        // 通过park(timeout)将当前线程挂起一段时间
        if (nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD)
            LockSupport.parkNanos(this, nanosTimeout);
        
        // 如果挂起等待的过程过程发生了中断,主动将线程节点从条件队列转移到同步队列
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
        nanosTimeout = deadline - System.nanoTime();
    }
    
    // 等到抢到锁资源才能继续执行.
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    // 条件队列的自我管理
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    // 如果支持抛出异常则抛出异常;如果不支持抛出异常则标记中断位
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
    
    // 中断导致等待提前结束,计算提前了多久结束
    long remaining = deadline - System.nanoTime(); // avoid overflow
    return (remaining <= initialNanos) ? remaining : Long.MIN_VALUE;
}
```