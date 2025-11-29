# juc-AQS-方法注释-AbstractQueuedSynchronizer



## acquire类

### acquireShared

一定能获得锁资源,那怕是通过挂起等待(不支持中断).

```text
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

#### tryAcquireShared


#### doAcquireShared

```text
private void doAcquireShared(int arg) {
    // 线程节点先队尾入队同步队列
    final Node node = addWaiter(Node.SHARED);
    
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```



## release类

### releaseShared

```text
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

### tryReleaseShared


#### doReleaseShared

Release action for shared mode -- signals successor and ensures propagation. 
(Note: For exclusive mode, release just amounts to calling unparkSuccessor of head if it needs signal.) 独占模式的release只需检查头节点的waitStatus,因为不存在并发修改头节点的waitStatus

```text
private void doReleaseShared() {
    /*
     * Ensure that a release propagates, even if there are other in-progress acquires/releases.  
     * This proceeds in the usual way of trying to unparkSuccessor of head if it needs signal. 
     * But if it does not, status is set to PROPAGATE to ensure that upon release, propagation continues.
     * Additionally, we must loop in case a new node is added while we are doing this. Also, unlike other uses of unparkSuccessor, we need to know if CAS to reset status fails, if so rechecking.
     */
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) //当存在并发releaseShared时,当前releaseShared竞争失败,再次循环尝试
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) // 当存在并发releaseShared再次尝试时,当前releaseShared再次尝试竞争又失败; 或 当存在并发acquireShared时,当前releaseShared再次尝试竞争又失败,需再次循环尝试
                continue;                // loop on failed CAS
        }
        
        
        if (h == head)                   // loop if head changed
            break;
    }
}
```


#### tryRelease

Attempts to set the state to reflect a release in exclusive mode.
仅仅设置state

#### fullyRelease(独占模式)(不支持中断)

```text
final int fullyRelease(Node node) {
    boolean failed = true;
    try {
        int savedState = getState();
        if (release(savedState)) { // 使用独占模式的release
            failed = false;
            return savedState;
        } else {
            throw new IllegalMonitorStateException();
        }
    } finally {
        if (failed)
            node.waitStatus = Node.CANCELLED; // 锁状态异常会导致条件节点变为取消节点
    }
}
```

```text
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```






