# juc-AQS-方法注释-AbstractQueuedSynchronizer

## 解释中断
线程发生阻塞通常指的是线程被挂起,无法被cpu调度.
线程被挂起常发生在获取synchronized的monitor失败,或者调用Object.wait,或者调用park方法时发生的.
所有锁实现挂起线程的底层方法都是通过如上3个方法.
只有在线程被挂起这段时间,如果线程被调用了interrupt()方法,此时线程结束挂起恢复调度并顺利执行完了方法体,此时我们就可以认为该锁方法支持中断.



## AbstractQueuedSynchronizer

### 核心方法-不支持中断
#### acquire(独占模式)(不支持中断)

一定能获得锁资源,那怕是通过挂起等待(不支持中断).

```text
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

#### tryAcquire(独占模式)

该方法需要子类实现.
该方法不能阻塞线程,仅仅是简单的根据状态判断并通过CAS完成资源的抢占.


#### addWaiter(不支持中断)

一定能向队列尾部成功插入一个node(必要时初始化队列).(不支持中断)


AQS的内部维护一个FIFO的双向链表队列,新增节点从队尾追加
这个队列永远不会让真正的线程节点成为头节点,头节点永远是一个逻辑上dummy-node(参见acquireQueued方法)

```text
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    // 先尝试enq的简化版(一次CAS)
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    // 失败了再尝试enq的完整版(无穷次循环CAS)
    enq(node);
    return node;
}



private Node enq(final Node node) {
    for (;;) {  // 通过无穷循环一定要将node插入到队列的尾部
        Node t = tail;
        if (t == null) { // 队列未初始化,先往队列中插入一个dummy head(dummy-node作为队列工作的启动点)
            if (compareAndSetHead(new Node())) // CAS先设置了dummy head
                tail = head; // 注意: 由于无法一个CAS操作tail/head,会让其他线程看到:head!=null但tail==null
        } else { // 队列已经初始化
            node.prev = t;
            if (compareAndSetTail(t, node)) { // CAS将入参node设置到队尾,如果失败再下轮循环中重试直到成功
                t.next = node; // 注意线程节点都是先设置prev再设置next
                return t;
            }
        }
    }
}

注意同步队列在尾部添加节点时:
线程节点先设置prev再CAS设置tail指针,最后设置next.(不用设置head指针)
    在CAS设置tail指针前设置prev成功后,同步反向队列和同步正向队列都是旧的,未变化.(此时tail还是旧值,无法找到未来的新尾节点)
    在CAS设置tail指针成功后且还未设置next前,同步反向队列是新的完整的,同步正向队列中还是旧尾节点(不完整的)
    在CAS设置tail指针后设置next成功后,同步反向队列和同步正向队列都是新的
初始化节点先设置head再设置tail
    初始化节点不做过多分析,先后无关紧要

总结来说,程节点都是优先保证同步反向队列


```

#### acquireQueued(不支持中断)

一定能获得锁资源,那怕是通过挂起等待(不支持中断).
但该方法可以通过返回值来表明挂起等待过程中是否曾被中断过.

为了防止cpu空转(避免忙等),只有第一个线程节点(head的下一位)才被允许通过cas来争夺锁资源,
其他的node需要通过park将自己的线程挂起,等待被通知后才能恢复线程调度.

关于中断
如果整个过程中没有发生过挂起等待,则返回值不准确,可以看作是方法刚结束时发生了中断而来不及记录

退出该方法后,逻辑上的线程节点不再继续保留在同步队列中(变成了逻辑上的dummy-node).

```text
boolean acquireQueued(final Node node, long arg)
    Acquires in exclusive uninterruptible mode for thread already in queue. Used by condition wait methods as well as acquire.
    Returns: true if interrupted while waiting(只计算因为park导致waiting过程中发生的中断)
    

final boolean acquireQueued(final Node node, long arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) { // 只有当当前节点变为第一个线程节点才能退出这个循环,其余时间只能挂起等待(中断也仅仅是提前唤醒线程,如果此时还不是第一个线程节点,会接着挂起等待)
            final Node p = node.predecessor();
            // 第一个线程节点的p是dummy-node,tryAcquire成功后,将第一个线程节点设置为head(初始化的dummy-node不存在了, 此时thread/prev都设置为null)
            // 第一线程释放锁资源后,仅仅将head.waitStatus清空默认值0,并没有改动队列的结构(此时head的thread/prev/waitStatus都默认值,类似于dummy-node)
            // 不管是第几个线程释放完锁资源,head都是一个逻辑上dummy-node(可能不是同一个对象了)
            if (p == head && tryAcquire(arg)) { // 前驱是head时,即当前节点是第一个线程节点,才尝试获取锁
                setHead(node); // 获取锁资源成功设置当前节点成为head(thread/prev都设置为null)(初始化的dummy-node不存在了)
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            // 因为tryAcquire失败,需要将前驱节点的waitStatus设置为SIGNAL,这样在自己挂起后才会被其他线程unpark
            // unpark/interrupt激活线程后,继续for循环去tryAcquire直到成功
            if (shouldParkAfterFailedAcquire(p, node) && // 检查是否需要挂起
                parkAndCheckInterrupt()) // interrupt导致提前退出park方法
                interrupted = true;
        }
    } finally {
        // 锁等待过程中发生过中断,此后不要需要这个节点了,需要移除(正常情况下,不能移除,后面可能还要调用newCondition时,重新使用这个node)
        if (failed)
            cancelAcquire(node);
    }
}
```

#### shouldParkAfterFailedAcquire(不支持中断)

主要用于 决定一个获取锁失败的线程是否应该进入阻塞（park）状态

AQS 的获取流程回顾
当线程尝试获取锁失败时（tryAcquire() 返回 false），它会进入同步队列，并进入一个循环：
    判断自己是否是队列的第一个有效等待节点（head.next）；
    如果是，则再次尝试 tryAcquire()；
    否则，调用 shouldParkAfterFailedAcquire() 决定是否需要 LockSupport.park() 进入阻塞。
也就是说，这个方法决定线程“是继续自旋还是进入休眠等待唤醒”。

设计意图:
安全地决定线程是否可以进入阻塞状态
保证：
    不会在还没设置好“唤醒链”的情况下提前阻塞；
    不会因为前驱节点被取消而“永远睡死”；
    不会重复唤醒或丢失唤醒信号；
    最终确保等待队列是健康的、连续的。

按照同步反向队列的顺序,在前驱节点上设置SIGNAL标志以便其他线程唤醒当前线程.
在设置的过程中如果发现前驱节点已经canceled,将该canceled节点先在同步反向队列中清除再在同步正向队列中清除.

```text
boolean shouldParkAfterFailedAcquire(Node pred, Node node)
    Checks and updates status for a node that failed to acquire. Returns true if thread should block. 
    This is the main signal control in all acquire loops. 
    Requires that pred == node.prev.
    Params:
        pred – node's predecessor holding status 
        node – the node
    Returns: true if thread should block(返回true表示可以将当前线程挂起)

将前驱节点的waitStatus设置为SIGNAL,即告知其他线程在释放锁资源时记得唤醒当前线程.

private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        /* This node has already set status asking a release to signal it, so it can safely park. */
        return true;
    if (ws > 0) { // 当前node的前节点取消了,重新构建FIFO,且先不park再次回到外层自旋中,再次尝试争夺锁资源
        /* Predecessor was cancelled. Skip over predecessors and indicate retry. */
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else { // 如果是同步队列,waitStatus没有不会有CONDITION,此时只能是0 or PROPAGATE.
        /*
         * waitStatus must be 0 or PROPAGATE.  
         * Indicate that we need a signal, but don't park yet.  Caller will need to retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}


逐行解释
前驱节点的状态，它决定了当前节点是否需要等待唤醒
if (ws == Node.SIGNAL)
如果前驱节点的状态是 SIGNAL，
    表示当前节点已经被正确地标记为“前驱释放时应唤醒我”。那么此时就可以安全地阻塞当前线程了。返回true.

if (ws > 0)
如果 ws > 0，也就是前驱的状态为 CANCELLED（被取消排队）：
    表明前驱节点已经无效，需要跳过所有已取消的节点，找到最近一个有效的前驱节点，重新连接队列(即移除了前面所有被取消的节点)。
    由于队列的前面发生了移除节点,当前节点有可能是第一个线程节点,所以返回false表示不要park,需要在外层的下次循环中重新判断,如果是第一个线程节点还要参与锁竞争
else
前驱状态为 0 或 PROPAGATE,说明前驱节点还没设置SIGNAL状态,此时需要通过 CAS 操作将其设置成 SIGNAL
    这样可以确保：当前驱节点释放锁时，会负责唤醒它的下一个节点(也就是当前线程)
    这里源码选择了返回false,在park前再竞争一下锁(不知道什么原因让作者不直接返回false)

```
#### cancelAcquire(不支持中断)

一句话总结：
    当线程在尝试获取锁时（acquireQueued 过程中）被中断或超时，
    它需要调用 cancelAcquire 将自己从AQS的同步队列中安全地移除。
也就是说，这个方法是 "退出排队" 的安全逻辑。

添加节点发生在队尾,CAS+重试可以不遗漏unpark信号.
唤醒是从队尾往前找,且不改动队列结构,且不涉及队尾.
cancelAcquire是将某个节点及其前驱节点移除,先修复prev队列后修复next队列

这里要移除节点,最担心的移除过程中miss signal(这也是这里的处理重点)

注意: 这里只清理当前节点及其前面的取消节点,之后的节点不处理.

按照同步反向队列的顺序,清理当前及其前驱节点中canceled的,先在同步反向队列中清除再在同步正向队列中清除.

```text
Cancels an [ongoing] attempt to acquire. (ongoing 指的是获取锁资源中发生了取消)

private void cancelAcquire(Node node) {
    if (node == null)
        return;

    node.thread = null;

    // 跳过所有已取消的前驱节点,找到最近的一个有效节点
    Node pred = node.prev;
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev; // 先清除canceled节点(修复同步反向队列)

    // 找到有效的前驱节点的原来后继
    Node predNext = pred.next;

    // 将当前节点标记为“已取消”
    node.waitStatus = Node.CANCELLED;

    // 如果当前节点是当前的tail,则cas设置新tail,不能留一个无效的node.(如果cas失败,证明已经有其他的非失效node设置为tail,当前节点的waitStatus也无用)
    if (node == tail && compareAndSetTail(node, pred)) {
        compareAndSetNext(pred, predNext, null); // pred为新tail,需要清理next字段值(修复同步反向队列)
    } else {
        int ws;
        // 如果pred不是head,且pred的状态是SIGNAL或者成功将其设置为为SIGNAL
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL ||
             (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
            pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0) // next.waitStatus <= 0时,next.waitStatus为SIGNAL/PROPAGATE(等价于都需要唤醒通知)
                compareAndSetNext(pred, predNext, next); // 这里才修复了同步正向队列
        } else { // 如果pred是head,直接unparkSuccessor
            unparkSuccessor(node); // 托底,将后面节点的线程唤醒,唤醒后的线程会按照其他方法的既定算法正确执行 (第一个线程节点/tail-CAS失败都会走这里)
        }

        node.next = node; // help GC
    }
}
```

#### unparkSuccessor(不支持中断)

````text

从队尾往前找当前节点后第一个需要唤醒的线程节点(过程中并没有改动队列的节点关系)

private void unparkSuccessor(Node node) {
    /*
     * If status is negative (i.e., possibly needing signal) try to clear in anticipation of signalling.  
     * It is OK if this fails or if status is changed by waiting thread.
     */
    int ws = node.waitStatus; // 在CAS前已经读取到下个node的线程设置的通知信息
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0); // 如果清除旧通知信息失败,证明其他线程在读取旧的信息后设置了新的信息,此时就不能清除新的信息

    /*
     * Thread to unpark is held in successor, which is normally just the next node.  But if cancelled or apparently null,
     * traverse backwards from tail to find the actual non-cancelled successor.
     */
    Node s = node.next;
    // s == null意味着next-node就是tail,此时s就为null
    // s.waitStatus > 0意味着next-node已经取消,需要从队尾往前找当前节点后第一个需要唤醒的线程节点,这样才不会miss-signal
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0) // 从队尾往前找离当前节点最近的需要唤醒的(waitStatus<=0)节点
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
````

#### hasQueuedPredecessors(不支持中断)

判断当前线程是否在同步队列中有前驱节点（即：自己是不是队列头的下一个等待者）。

只要head(dummy-node)后有一个非当前线程的节点,也就意味至少有一个前驱节点,即有前驱节点.


这个方法是用来判断核心问题：
“当前线程是否需要排队？”

换句话说：
如果当前线程前面有人排队，那就不应该直接尝试获取锁；
如果当前线程是队列中第一个候选者，那就可以去竞争锁。

```text
public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized
    // before tail and on head.next being accurate if the current
    // thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}

h != t
当h==t时,队列为空,此时当前节点没有前驱节点.
(s = h.next) == null
队列的head是dummy-node,s为head的下个节点即为真实线程的节点,如果真实线程的节点为null,表示队列中没有其他线程节点.
s.thread != Thread.currentThread()
暗含了前面的(s = h.next) != null,即第一个线程节点存在,此时该节点的线程不是当前线程,那就意味着前面有其他线程在等待.
```


#### release(不支持中断)

一定能release n个permit.(jdk库都不支持中断)

release相关的方法有2个,即对应独占模式和共享模式,需要子类实现.
```text
tryRelease(int)	        尝试释放独占锁	    ✔️（独占模式）
tryReleaseShared(int)	尝试释放共享锁	    ✔️（共享模式）
```

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

### 工具方法

#### isOnSyncQueue

入参的node初始位于条件队列中.

Returns true if a node, always one that was initially placed on a condition queue, is now waiting to reacquire on sync queue.

```text
final boolean isOnSyncQueue(Node node) {
    if (node.waitStatus == Node.CONDITION || node.prev == null)
        return false;
    if (node.next != null) // If has successor, it must be on queue
        return true;
    /*
     * node.prev can be non-null, but not yet on queue because the CAS to place it on queue can fail. So we have to traverse from tail to make sure it actually made it.  
     * It will always be near the tail in calls to this method, and unless the CAS failed (which is unlikely), it will be there, so we hardly ever traverse much.
     */
     // 反向遍历同步队列,确定当前节点是否在同步队列中(处理)
    return findNodeFromTail(node);
}


node.prev == null 
    表示该节点还未链接到同步队列(因为同步队列存在逻辑上的dummy-node节点,所以必然同步队列中的所有线程节点的prev都非null,而并非所有的线程节点的next都有值,比如线程节点是尾节点)
node.next != null
    如果next不为空,说明该节点已经在同步队列中(因为条件队列中节点是不设置next)   
    
return findNodeFromTail(node)   
    如果上面两种情况都不确定（prev != null 且 next == null）,就说明节点可能刚刚在 signal() 转移过程中 
```

#### transferAfterCancelledWait


Transfers node, if necessary, to sync queue after a cancelled wait. (将等待节点转移到同步队列中)
Returns true if thread was cancelled before being signalled.(如果转移)

```text
final boolean transferAfterCancelledWait(Node node) {
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
        enq(node);
        return true;
    }
    /*
     * If we lost out to a signal(), then we can't proceed until it finishes its enq().  Cancelling during an incomplete transfer is both rare and transient, so just spin.
     */
    while (!isOnSyncQueue(node))
        Thread.yield();
    return false;
}
```


### 核心方法-支持中断

#### acquireInterruptibly(独占模式)(支持中断)

acquire方法的支持中断版本

```text
public final void acquireInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (!tryAcquire(arg)) 
        doAcquireInterruptibly(arg); // 尝试失败则通过等待队列完成
}
```

#### doAcquireInterruptibly(独占模式)(支持中断)

acquireQueued方法的支持中断版本.直接抛出中断异常而非以返回值告知调用方

```text
private void doAcquireInterruptibly(int arg) throws InterruptedException {
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt()) // 如果当前节点的前节点标记好了SIGNAL后才能挂起当前线程
                throw new InterruptedException(); // 如果当前线程是因为中断才退出park的话,直接抛出中断异常
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```