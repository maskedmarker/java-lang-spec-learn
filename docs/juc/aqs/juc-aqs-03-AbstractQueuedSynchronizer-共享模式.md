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

### tryAcquireShared
该方法需要子类实现.
该方法不能阻塞线程

### doAcquireShared

⚠️第一线程节点抢占到锁资源后,还要再连带唤醒一个第二节点线程.
⚠️第二节点线程在锁资源充足的情况下还会引发连带唤醒一个第三节点线程,以此类推.
⚠️这个连带唤醒在锁资源耗尽后就停止了,这样避免了锁资源还有而线程未被唤醒的场景.


```text
private void doAcquireShared(int arg) {
    // 线程节点先队尾入队同步队列
    final Node node = addWaiter(Node.SHARED);
    
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            // 💯在共享模式下,还是只有第一线程节点能才有尝试抢占锁资源的权力.(并不是靠前的n个都有尝试权)
            if (p == head) {
                // 独占模式下,一次tryAcquire可能就会将锁资源占用完;共享模式下,一次tryAcquireShared通常不会占用完
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    // 💯通过cascading的级联唤醒形式,来实现类似于靠前的n个都获得尝试权,同时还维持了FIFO的承诺
                    setHeadAndPropagate(node, r);
                    p.next = null;
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&  // 尝试抢占锁资源失败,在node.prev.waitStatus标记SIGNAL后挂起等待
                parkAndCheckInterrupt())
                interrupted = true;   // 记录挂起等待中发生的中断
        }
    } finally {
        if (failed)
            cancelAcquire(node); // 锁状态异常后取消等待
    }
}

// "靠前的n个都有尝试权"中的n无法量化,需要等价转换为 "在锁资源还有剩余的情况下唤醒更多的线程直到锁资源没有剩余".
```

### setHeadAndPropagate

这个函数做的事情有两件:
1. 在获取共享锁成功后,设置head节点
2. 根据调用tryAcquireShared返回的状态以及节点本身的等待状态来判断是否要需要唤醒后继线程

⚠️ 如果node.next是独占节点,级联唤醒将停止.

```text
private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head; // 记录旧头节点
    setHead(node);  // 设置新头节点
    
    
    if (propagate > 0 ||                         // 锁资源还未使用完,尝试一下唤醒
        h == null || h.waitStatus < 0 ||         // 锁资源即使使用完了,根据旧head判断后面还有需要唤醒的线程,尝试一下唤醒
        (h = head) == null || h.waitStatus < 0) {   // 锁资源即使使用完了,根据新head判断还有需要唤醒的线程,尝试一下唤醒
        
        Node s = node.next;
        
        // (s == null)则node此时是尾节点(要防止遗漏可能马上就有的新尾节点);
        // 本方法主要是为了实现共享模式下的级联唤醒形式 s.isShared()判断是主体,(s == null)是edge-case
        if (s == null || s.isShared())
            doReleaseShared(); // 唤醒
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

子类实现该方法
不能发生挂起等待
锁状态异常时支持抛出状态异常

### doReleaseShared

Release action for shared mode -- signals successor and ensures propagation. 
(Note: For exclusive mode, release just amounts to calling unparkSuccessor of head if it needs signal.) 独占模式的release只需检查头节点的waitStatus,因为不存在并发修改头节点的waitStatus


⚠️共享模式下,多个线程并发执行releaseShared,会出现被唤醒的线程数小于释放锁的线程数.(所以在共享模式下,在acquire端增加级联唤醒的能力,减少还有锁资源而需要唤醒而未唤醒的概率)
⚠️doReleaseShared不区分共享节点和独占节点.(独占模式完全不区分共享节点和独占节点)

```text
private void doReleaseShared() {
    /*
     * Ensure that a release propagates, even if there are other in-progress acquires/releases.  
     * This proceeds in the usual way of trying to unparkSuccessor of head if it needs signal. 
     * But if it does not, status is set to PROPAGATE to ensure that upon release, propagation continues.
     * Additionally, we must loop in case a new node is added while we are doing this. Also, unlike other uses of unparkSuccessor, we need to know if CAS to reset status fails, if so rechecking.
     */
    for (;;) {
        // 记录旧head
        Node h = head;
        
        // 如果同步队列存在线程节点
        if (h != null && h != tail) {
            // ws不可能是CANCELED/CONDITION,只剩下的可能性为0/SIGNAL/PROPAGATE
            int ws = h.waitStatus;
            
            // head的waitStatus 要么清空SIGNAL并唤醒一个线程;要么设置0->PROPAGATE
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) //当存在并发releaseShared时,当前releaseShared竞争失败,再次循环尝试
                    continue;            // loop to recheck cases
                
                // 唤醒队列第一线程节点,体现了FIFO.
                unparkSuccessor(h);
            }
            else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) // 当存在并发releaseShared再次尝试时,当前releaseShared再次尝试竞争又失败; 或 当存在并发acquireShared时,当前releaseShared再次尝试竞争又失败,需再次循环尝试
                continue;                // loop on failed CAS
        }
        
        // 同一个head上多次重复操作,由于无法叠加,效果上等价于执行一次.  (在同一个head上,多次执行unparkSuccessor和cas是无法叠加的)
        if (h == head)                   // loop if head changed
            break;
    }
}

unparkSuccessor(h)也会清空SIGNAL,而这里还看似多余地提前compareAndSetWaitStatus(h, Node.SIGNAL, 0),主要是为了在并发中确认代码执行路径.


假如在同一个head上发生多次执行unparkSuccessor,意味着节点h后最近的正常节点是同一个节点,且由于unpark无法叠加,效果上等价于执行一次unparkSuccessor.(如果并发发生取消操作,会有不同)
假如在同一个head上发生多次执行cas(0, Node.PROPAGATE),由于无法叠加,效果上等价于执行一次cas(0, Node.PROPAGATE).


在同一个head上
    最多只支持2个并发的releaseShared,因为只能在同一个head的waitStatus上要么设置SIGNAL->0要么设置0->PROPAGATE
    同时只有设置SIGNAL->0的那个releaseShared线程能够唤醒一个线程,而另一个仅仅做了PROPAGATE的标记.
    如果有n个并发releaseShared,且n>=3, 则存在(n-2)个线程在releaseShared的doReleaseShared中什么也不做就退出方法了.
如果被唤醒的线程执行的比较快,一个doReleaseShared中会执行多次 (清空SIGNAL并唤醒一个线程 或者 设置0->PROPAGATE) (多唤醒至少是无害的)
```






