# juc-AQS-方法注释-ConditionObject


## 接口Condition

Condition支持2类方法,即await类和signal类.

Condition的javadoc描述了await类和signal类方法的实现

### signal

通常情况下,要求调用signal方法的当前线程持有锁.
至少在JDK和常见的库中,Condition都是被互斥锁保护的.

```text
Wakes up one waiting thread.
If any threads are waiting on this condition then one is selected for waking up. That thread must then re-acquire the lock before returning from await.

Implementation Considerations
An implementation may (and typically does) require that the current thread hold the lock associated with this Condition when this method is called. 
Implementations must document this precondition and any actions taken if the lock is not held. Typically, an exception such as IllegalMonitorStateException will be thrown.
```
### await

要求调用await方法的当前线程必须持有锁.
至少在JDK和常见的库中,调用await方法的当前线程持有锁,且是互斥锁.

```text
Causes the current thread to wait until it is signalled or interrupted.
The lock associated with this Condition is atomically released and the current thread becomes disabled for thread scheduling purposes and lies dormant until one of four things happens:
    Some other thread invokes the signal method for this Condition and the current thread happens to be chosen as the thread to be awakened; or
    Some other thread invokes the signalAll method for this Condition; or
    Some other thread interrupts the current thread, and interruption of thread suspension is supported; or
    A "spurious wakeup" occurs.

In all cases, before this method can return the current thread must re-acquire the lock associated with this condition. When the thread returns it is guaranteed to hold this lock.

Implementation Considerations
The current thread is assumed to hold the lock associated with this Condition when this method is called. 
It is up to the implementation to determine if this is the case and if not, how to respond. Typically, an exception will be thrown (such as IllegalMonitorStateException) and the implementation must document that fact.
An implementation can favor responding to an interrupt over normal method return in response to a signal. In that case the implementation must ensure that the signal is redirected to another waiting thread, if there is one.
(如果中断和signal同时发生,抛出异常前要将signal is redirected to another waiting thread)

```


## ConditionObject

ConditionObject的条件队列也是FIFO队列.使用nextWaiter字段将条件队列节点串起来,使用lastWaiter标记条件队列的队尾,使用firstWaiter标记条件队列的队首.
ConditionObject的条件队列是普普通通的队列,并不是防并发的,因为:至少在JDK的库中,Condition都是被互斥锁保护的.(常见库中也是这样使用的)
条件队列的节点插入不存在并发.条件队列的节点移除是CAS-waitStatus的逻辑移除也不存在并发问题




### awaitUninterruptibly(不支持中断)

awaitUninterruptibly不支持中断,但是在退出方法时会通过线程中断位来表明在执行该方法的整个期间是否发生过中断.


```text
public final void awaitUninterruptibly() {
    // 将线程节点加入条件队列
    Node node = addConditionWaiter();
    
    // 释放持有的锁资源()
    int savedState = fullyRelease(node);
    
    boolean interrupted = false;
    
    // 等待signal方法将该线程的节点从等待队列转移到同步队列
    while (!isOnSyncQueue(node)) {
        // 挂起当前线程
        LockSupport.park(this);
        
        // 记录挂起阶段是否有中断发生
        if (Thread.interrupted())
            interrupted = true;
    }
    
    // 退出await方法需要先获取到锁
    // 如果重新获取锁的等待过程发生了中断 或者在等待条件的过程中发生了中断,通过线程中断位来表明
    if (acquireQueued(node, savedState) || interrupted)
        selfInterrupt();
}
```

### await(支持中断)

await支持中断


```text
public final void await() throws InterruptedException {
    // 此时还没有释放锁,所以不用去抢锁,直接直接抛出中断异常退出await(不用再去条件队列中等待了)
    if (Thread.interrupted())
        throw new InterruptedException();
    
    // 将线程节点加入条件队列    
    Node node = addConditionWaiter();
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

### await(支持超时+中断)

```text
public final boolean await(long time, TimeUnit unit) throws InterruptedException {
    long nanosTimeout = unit.toNanos(time);
    if (Thread.interrupted())
        throw new InterruptedException();
        
    Node node = addConditionWaiter();
    int savedState = fullyRelease(node);
    final long deadline = System.nanoTime() + nanosTimeout;
    boolean timedout = false;
    int interruptMode = 0;
    
    while (!isOnSyncQueue(node)) {
        if (nanosTimeout <= 0L) {
            timedout = transferAfterCancelledWait(node);
            break;
        }
        if (nanosTimeout >= spinForTimeoutThreshold)
            LockSupport.parkNanos(this, nanosTimeout);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
        nanosTimeout = deadline - System.nanoTime();
    }
    
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null)
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
    return !timedout;
}
```


### signal

⚠️ 持有锁线程才能调用signal

一定能将一个条件节点(如果有的话)转移到同步等待队列中.

```text
Moves the longest-waiting thread, if one exists, from the wait queue for this condition to the wait queue for the owning lock.

public final void signal() {
    // 持有锁线程才能调用signal
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
        
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}


private void doSignal(Node first) {
    do {
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&  // 取消操作比signal先发生
             (first = firstWaiter) != null);
}

取消操作比signal先发生,将下一个条件节点转移到同步等待队列
```


### addConditionWaiter

Adds a new waiter to wait queue.
Returns:its new wait node

ConditionObject的条件队列也是FIFO队列.
使用nextWaiter字段将条件队列节点串起来,使用lastWaiter标记条件队列的队尾,使用firstWaiter标记条件队列的队首.

addConditionWaiter被await方法调用.await方法仅被持有互斥锁的线程调用,不存在并发.

```text
private Node addConditionWaiter() {
    Node t = lastWaiter;
    
    // If lastWaiter is cancelled, clean out.
    if (t != null && t.waitStatus != Node.CONDITION) {
        // 在新增新条件节点时清理逻辑出队的节点
        unlinkCancelledWaiters();
        // 清理后,将新的lastWaiter赋值给t
        t = lastWaiter;
    }
    
    // 条件队列是FIFO队列
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    if (t == null)
        firstWaiter = node; // 如果条件队列已空,新节点就是队首firstWaiter
    else
        t.nextWaiter = node; // 如果条件队列不空,在条件队列的尾节点后追加一个新节点
    
    lastWaiter = node; // 新节点成为新的尾节点
    
    return node;
}
```


### unlinkCancelledWaiters

Unlinks cancelled waiter nodes from condition queue. Called only while holding lock.
This is called when cancellation occurred during condition wait, and upon insertion of a new waiter when lastWaiter is seen to have been cancelled.

该方法用于从条件队列中移除已取消的等待节点,仅在持有锁的情况下才能调用该方法。

条件队列的自修复机制
调用的时机: 在等待期间发生(超时/中断)取消，或在插入新等待者时发现尾节点已被取消时触发.这样在没有signal信号唤醒时防止内存泄漏。

```text
private void unlinkCancelledWaiters() {
    // t指向遍历过程中的当前节点
    Node t = firstWaiter;
    // trail指向遍历过程中碰到的最后一个条件节点
    Node trail = null;
    
    while (t != null) {
        // next是当前节点的下个节点
        Node next = t.nextWaiter;
        
        if (t.waitStatus != Node.CONDITION) { // 如果当前节点不是已取消的节点
            t.nextWaiter = null; // 当前节点需要移除了,断开引用来帮助GC(如果不主动断开引用,且长时间Condition的signal没有到来,会一直强引用,导致无法gc)
            
            if (trail == null)  // 之前没碰到过条件节点且当前节点也不是条件节点,才能放心更新队首指针(假如当前节点不是条件节点但其前面有条件节点,此时就不能仅凭当前节点也不是条件节点盲目就更新队首指针)
                firstWaiter = next;
            else
                trail.nextWaiter = next; // 之前碰到过条件节点且当前节点也不是条件节点,让最后一个条件节点指向next,剔除当前节点

            if (next == null)
                lastWaiter = trail; // 若当前节点的是队尾,更新移动队尾指针
        }
        else
            trail = t; // tail指向发现的最后一个条件节点
        
        t = next; // 遍历下一个节点
    }
}

while循环遍历等待队列的各个节点,移除非条件节点.并同步更新队首尾指针.
什么时候可以更新队首指针?
    之前没碰到过条件节点且当前节点也不是条件节点,才能放心更新队首指针
        假如当前节点不是条件节点但其前面有条件节点,此时队首指针至少在最后一个条件节点处
        更新: 队首指针应指下一个节点
什么时候可以更新队尾指针?
    当前节点是最后一个节点,并且是非条件节点. 
        假如当前节点是最后一个节点且是条件节点,此时就无需更新队尾指针
        更新: 队尾指针应指向遍历中发现的最后一个条件节点
在整个循环中,
    firstWaiter保持了指向首个可能是条件节点处
```


### 节点从条件队列转移到同步队列
transferAfterCancelledWait指的是条件节点被中断/超时取消了挂起等待,进入同步队列中来抢占锁.
transferForSignal指的是条件节点被通知条件满足结束挂起等待,进入同步队列中来抢占锁.

⚠️ transferAfterCancelledWait和transferForSignal存在并发,所以通过CAS(CONDITION, 0)来决定先后.
⚠️ 由于条件队列使用nextWaiter和waitStatus字段,同步等待队列使用prev/next和waitStatus字段, 所以只需要通过改变waitStatus值立即实现条件节点的逻辑出队,实际出队可以由延迟的unlinkCancelledWaiters来清除.


#### transferAfterCancelledWait

Transfers node, if necessary, to sync queue after a cancelled wait. Returns true if thread was cancelled before being signalled.
Returns: true if cancelled before the node was signalled

条件队列的节点线程被中断/超时打断了挂起,打算不再等条件了直接进入同步队列中来抢占锁.


```text
入参node此时应该处于条件队列中(也可能因为并发的signal先人一步,已经在将node转移到同步队列中)

final boolean transferAfterCancelledWait(Node node) {
    // 通过CAS来决定是(超时/中断)cancel先发生还是signal先发生
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
        // 由于条件队列使用nextWaiter和waitStatus字段,同步等待队列使用prev/next和waitStatus字段, 所以只需要通过改变waitStatus值立即实现条件节点的逻辑出队,实际的出队可以延迟处理.
        enq(node);
        return true;
    }
    
    // 等待正在执行的signal方法结束(通常等待时间很短)
    while (!isOnSyncQueue(node))
        Thread.yield();
    
    return false;
}
```

#### transferForSignal

Transfers a node from a condition queue onto sync queue. Returns true if successful.
Returns: true if successfully transferred (else the node was cancelled before signal)

条件队列的节点线程被通知结束挂起,并进入同步队列中来抢占锁.

正常情况下将node入队同步队列并为node.prev.waitStatus设置SIGNAL就行了.

```text
入参node此时应该处于条件队列中(也可能因为并发的cancel先人一步,已经在将node转移到同步队列中)

final boolean transferForSignal(Node node) {
    
    // 通过CAS来决定是(超时/中断)cancel先发生还是signal先发生
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;

    // 将node入队到同步队列,并将node.prev.waitStatus设置为SIGNAL
    Node p = enq(node);
    int ws = p.waitStatus;
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
        // 当无法在node.prev.waitStatus上设置SIGNAL时,存在并发操作,当前节点线程可能不发被唤醒. 只能使用托底的简化操作,保证node.thread恢复调度去抢占锁
        LockSupport.unpark(node.thread);
    
    return true;
}

并发操作比较复杂
比如前节点取消且cancelAcquire也结束了(此时需要这里unpark)也肯能没有结束(cancelAcquire中可能会执行unpark),在这里判断是非常困难的.
```