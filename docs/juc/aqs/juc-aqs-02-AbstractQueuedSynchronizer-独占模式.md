# juc-AQS-方法注释-AbstractQueuedSynchronizer


## 核心方法-不支持中断

### acquire

一定能获得锁资源,那怕是通过挂起等待(不支持中断).


```text
public final void acquire(int arg) {
    // ⚠️尝试抢占锁成功的线程不会在同步等待队列中添加节点
    if (!tryAcquire(arg) &&
        // 抢占失败的线程需要进入FIFO同步等待队列中等待,然后按先后顺序去尝试抢占锁,只有最靠前的线程才能尝试.
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        // acquireQueued不支持中断,需要手动恢复中断标识
        selfInterrupt();
}
```

### tryAcquire(独占模式)

该方法需要子类实现.
该方法不能阻塞线程,仅仅是简单的根据state字段(并不会也不应该考虑aqs的同步队列的状态)判断并通过CAS完成资源的抢占.


Attempts to acquire in exclusive mode. This method should query if the state of the object permits it to be acquired in the exclusive mode, and if so to acquire it.



### acquireQueued(不支持中断)

一定能获得锁资源,那怕是通过挂起等待(不支持中断).
但该方法可以通过返回值来表明挂起等待过程中是否曾被中断过.

为了防止cpu空转(避免忙等),只有第一个线程节点(head的下一位)才被允许通过cas来争夺锁资源,
其他的node需要通过park将自己的线程挂起,等待被通知后才能恢复线程调度.

关于中断
如果整个过程中没有发生过挂起等待,则返回值不准确,可以看作是方法刚结束时发生了中断而来不及记录

退出该方法后,线程节点变成了逻辑上的dummy-node,不再是线程节点了.

```text
boolean acquireQueued(final Node node, long arg)
    Acquires in exclusive uninterruptible mode for thread already in queue. Used by condition wait methods as well as acquire.
    Returns: true if interrupted while waiting(只计算因为park导致waiting过程中发生的中断)
    

final boolean acquireQueued(final Node node, long arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        
        // 为了实现FIFO,只有当当前节点变为第一个线程节点才能去尝试抢占锁资源,其余时间只能等待(中断也仅仅是提前唤醒线程,如果此时还不是第一个线程节点,会接着挂起等待)
        for (;;) {
            final Node p = node.predecessor();
            
            // 第一个线程节点的前驱节点是dummy-node,tryAcquire成功后,将第一个线程节点设置为head(初始化的dummy-node不存在了, 此时thread/prev都设置为null)
            // 第一线程释放锁资源后,仅仅将head.waitStatus清空默认值0,并没有改动队列的结构(此时head的thread/prev/waitStatus都默认值,类似于dummy-node)
            // 不管是第几个线程释放完锁资源,head都是一个逻辑上dummy-node(可能不是同一个对象了)
            if (p == head && tryAcquire(arg)) { // 前驱是head时,即当前节点是第一个线程节点,才尝试获取锁
                setHead(node); // 获取锁资源成功设置当前节点成为head(thread/prev都设置为null)(初始化的dummy-node不存在了)(保留了waitStatus,因为后续节点还要用)
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            
            // 因为tryAcquire失败,需要将前驱节点的waitStatus设置为SIGNAL,这样在当前线程挂起后,其他线程看到这个信息才会unpark当前线程
            // unpark/interrupt激活线程后,继续for循环去tryAcquire直到成功
            if (shouldParkAfterFailedAcquire(p, node) && // 检查是否需要挂起
                parkAndCheckInterrupt()) // interrupt导致提前退出park方法,及时清除中断位才能让下次park挂起线程
                interrupted = true; // 因为中断位被清除,需要用独立变量来记录一下
        }
    } finally {
        // 由于支持tryAcquire抛出异常,所以存在failed==true的场景. 当发生抛出异常时,意味着此后不要需要这个节点了,需要移除.
        // 在现有的代码实现中,tryAcquire常在发现锁状态异常时抛出IllegalMonitorStateException,不支持某些操作时抛出UnsupportedOperationException,没见过在这里抛出中断异常
        if (failed)
            cancelAcquire(node);
    }
}
```

### shouldParkAfterFailedAcquire(不支持中断)

This is the main signal control in all acquire loops.
Checks and updates status for a node that failed to acquire. Returns true if thread should block.

⚠️当前方法是acquire-loop的核心控制节点,用来控制抢占锁失败的节点线程是否应该挂起.本方法应该放在acquire-loop的上下文理解,不能脱离这个上下文发散思考.💯
⚠️这里是设置node.prev.waitStatus为SIGNAL的一个主要地方.

决定一个抢占锁失败的线程是否应该进入挂起状态.
按照整体设计,要保证在prev队列中,离当前节点最近的正常前节点的waitStatus为SIGNAL,才能挂起.因为节点支持(超时/中断)取消操作,所以一定是正常前节点.

在设置的过程中顺便把紧邻的取消节点清除掉.
node.waitStatus=SIGNAL指的是node节点后有节点线程需要唤醒.(node节点后指的并不一定是下一个节点)

```text
确保前驱节点的waitStatus设置为SIGNAL(即告知其他线程在释放锁资源时记得唤醒当前线程),否则一直返回false让acquire-loop接着尝试以确保前驱节点的waitStatus为SIGNAL.

// 当前方法都是被acquire类方法调用的,所以是同步队列的节点,waitStatus不会有CONDITION
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        // 前节点被设置了SIGNAL,就肯定能被其他线程唤醒,可以安心挂起
        return true;
        
    
    if (ws > 0) { // 只有CANCELED是大于零的.
        // ⚠️穿插实现队列的自我管理(先设置prev再设置pred.next,保证整体设计中prev队列的实时完整性,next队列并不非实时正确) 因为没有使用排他锁,如果发生了新的取消操作,就会出现有取消节点没有清理(最终会收敛到全部被清除)
        // 将当前正常节点与最近的前正常节点之间的取消节点清理掉(由于2个正常节点间的节点集合不存在交集,所以并发执行这段while代码也不会并发问题)
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
        
        // if(ws > 0)这段逻辑就是为了在循环中穿插实现队列的自我管理,不能挂起线程
        return false;
    } else { // 此时只能是0/PROPAGATE.
        // ⚠️ 这里是设置前驱节点waitStatus为SIGNAL的主要地方
        // waitStatus==0表示还未被设置过,可放心被设置为SIGNAL,以便让其他线程看到后唤醒本线程; waitStatus==PROPAGATE表示需要将unpark向后传递以便将后面的线程唤醒,将PROPAGATE改为SIGNAL是等价的
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL); 
        
        // cas如果失败,只可能是pred正在取消等待
        // cas可能成功也要在下一轮中再抢一次锁(这里体现了设计者使用状态机的设计思路来处理代码,else就是用来设置SIGNAL的并不关心cas是否成功💯)
        return false;
    }
}

挂起的整体思路是:
在一轮循环中先对pred.waitStatus设置为Node.SIGNAL,再在下一轮循环中确认.
至于清除prev队列的取消节点,这个是为了队列的自我管理,需要穿插在循环中来做,与方法主体无关,自然不能返回true.
```





### release(不应该支持中断)

必定能唤醒一个同步队列最靠前的节点线程(如果存在的话).
由于是独占模式,不存在并发release
整个release不会发生阻塞,所以不要考虑中断

```text
public final boolean release(int arg) {
    // tryRelease不应该支持中断
    if (tryRelease(arg)) {
        // 为了FIFO,唤醒头节点后最近的一个正常节点线程
        Node h = head;
        
        if (h != null && h.waitStatus != 0) 
            unparkSuccessor(h); // 按prev队列从后往前找一个离h最近的正常节点线程,并唤醒.
        return true;
    }
    return false;
}

// (h != null && h.waitStatus != 0) 
表明同步等待队列已经初始化,且后面有线程需要唤醒.
存在(h.waitStatus != 0)但已经没有线程节点.(重要的是体现了设计者使用状态机来处理并发问题)

备注: 
节点发生取消操作时,第一线程节点会将unparkSuccessor,并将node.waitStatus清空为0;中间节点会保证node.prev.waitStatus为SIGNAL;尾节点直接cas-tail出队.
```

### tryRelease(不应该支持中断)

Attempts to set the state to reflect a release in exclusive mode.
整个release不会发生阻塞,所以不要考虑中断,所以tryRelease不应该支持中断.应该支持:当发现锁状态异常时抛出状态异常

### fullyRelease(不支持中断)

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

