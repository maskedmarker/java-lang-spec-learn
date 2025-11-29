# juc-AQS-方法注释-共享模式



## AbstractQueuedSynchronizer

### 核心方法
#### acquire(独占模式)(支持中断)

一定能获得锁资源,那怕是通过挂起等待(不支持中断).


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
            // unpark激活线程后,继续for循环去tryAcquire直到成功
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
    它需要调用 cancelAcquire 将自己从 AQS 的 等待队列 中安全地移除。
也就是说，这个方法是 "退出排队" 的安全逻辑。

添加节点发生在队尾,CAS+重试可以不遗漏unpark信号.
唤醒是从队尾往前找,且不改动队列结构,且不涉及队尾.
cancelAcquire是将某个节点及其前驱节点移除,先修复prev队列后修复next队列

这里要移除节点,最担心的移除过程中miss signal(这也是这里的处理重点)

注意: 这里只清理当前节点及其前面的取消节点,之后的节点不处理.

```text
private void cancelAcquire(Node node) {
    if (node == null)
        return;

    node.thread = null;

    // 跳过所有已取消的前驱节点,找到最近的一个有效节点
    Node pred = node.prev;
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev; // 先修复prev队列

    // 找到有效的前驱节点的原来后继
    Node predNext = pred.next;

    // 将当前节点标记为“已取消”
    node.waitStatus = Node.CANCELLED;

    // 如果当前节点是当前的tail,则cas设置新tail,不能留一个无效的node.(如果cas失败,证明已经有其他的非失效node设置为tail,当前节点的waitStatus也无用)
    if (node == tail && compareAndSetTail(node, pred)) {
        compareAndSetNext(pred, predNext, null); // pred为tail,需要清理next字段值
    } else {
        int ws;
        // 如果pred是head,直接走unparkSuccessor
        // 如果pred不是head,且pred的状态是SIGNAL或者成功将其设置为为SIGNAL
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL ||
             (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
            pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0) // next.waitStatus <= 0时,next.waitStatus为SIGNAL/PROPAGATE(等价于都需要唤醒通知)
                compareAndSetNext(pred, predNext, next); // 这里才修复了next队列
        } else {
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

## ConditionObject

```text
public class ConditionObject implements Condition, java.io.Serializable {
    /** First node of condition queue. */
    private transient Node firstWaiter;
    /** Last node of condition queue. */
    private transient Node lastWaiter;
    
    // ...
}


```


### await(支持中断)

类似于Object.wait(),但支持中断.


同步队列：保存正在等待获取锁的线程；
条件队列：保存调用了 await() 的线程（等待某个条件成立）。
当线程调用 await() 时，它会被封装为一个节点加入条件队列；如果线程被中断或被 signal() 唤醒，就需要将它转移到同步队列或移除无效节点。

````text


public final void await() throws InterruptedException {
    // await支持响应中断,先判断中断位
    if (Thread.interrupted())
        throw new InterruptedException();
    // 为当前线程创建一个新条件节点,并插入到条件队列的队尾
    Node node = addConditionWaiter();
    // 线程在进入条件等待前，必须完全释放所有的资源(而非一次的资源,比如重入锁的大于一的monitor-counter),否则其他线程无法获得锁,也就无法调用 signal() 唤醒它；
    long savedState = fullyRelease(node);
    
    int interruptMode = 0;
    // 如果当前线程的节点在条件队列时,一直挂起当前线程,直到当前线程的节点
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this); // 中断会唤醒线程
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) // 此时park挂起线程的等待被中断打断了
            break;
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
````


### addConditionWaiter

Adds a new waiter to wait queue.
Returns:its new wait node

Node.nextWaiter是用于实现等待队列,该队列是单向的(同步队列是双向的,可以看作同步正向队列和同步反向队列)
新节点追加在等待队列的队尾节点后.

```text
private Node addConditionWaiter() {
    Node t = lastWaiter;
    // If lastWaiter is cancelled, clean out.
    if (t != null && t.waitStatus != Node.CONDITION) {
        unlinkCancelledWaiters();
        // 清理后,将新的lastWaiter赋值给t
        t = lastWaiter;
    }
    Node node = new Node(Thread.currentThread(), Node.CONDITION);
    if (t == null)
        firstWaiter = node; // 等待队列的队首指针是firstWaiter
    else
        t.nextWaiter = node; // 在等待队列的尾节点后追加一个新节点
    lastWaiter = node; // 新节点成为新的尾节点
    return node;
}
```

### unlinkCancelledWaiters

Unlinks cancelled waiter nodes from condition queue. Called only while holding lock.
This is called when cancellation occurred during condition wait, and upon insertion of a new waiter when lastWaiter is seen to have been cancelled.

该方法用于从条件队列中移除已取消的等待节点,仅在持有锁的情况下才能调用该方法。

自修复机制
调用的时机:
    在条件队列的队尾插入新节点时,发现当前队尾节点已被取消时,顺带清除一下整个队列中的无效节点(非条件节点)
它在等待期间发生取消，或在插入新等待者时发现尾节点已被取消时触发,此方法的目的是在没有 signal 信号唤醒时防止垃圾节点（内存泄漏）。
虽然它可能需要遍历整个队列，但只在超时或取消等少数情况下执行,它会一次性清理所有无效节点，避免在大量取消的情况下多次遍历队列。

```text
private void unlinkCancelledWaiters() {
    // t指向遍历过程中的当前节点
    Node t = firstWaiter;
    // trail指向遍历过程中碰到的最后一个条件节点
    Node trail = null;
    while (t != null) {
        // next是当前节点的下个节点
        Node next = t.nextWaiter;
        
        if (t.waitStatus != Node.CONDITION) { // 如果当前节点是已取消的节点
            t.nextWaiter = null; // 当前节点需要移除了,断开引用来帮助GC(如果不主动断开引用,且长时间Condition的signal没有到来,会一直强引用,导致无法gc)
            
            if (trail == null)  // 之前没碰到过条件节点且当前节点也不是条件节点,才能放心更新队首指针(假如当前节点不是条件节点但其前面有条件节点,此时就不能仅凭当前节点也不是条件节点盲目就更新队首指针)
                firstWaiter = next;
            else
                trail.nextWaiter = next; // 之前碰到过条件节点且当前节点也不是条件节点,让最后一个条件节点指向next

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

### signal

```text

public final void signal() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first); // 唤醒等待队列的首节点(因为等待最久)
}
```

```text
private void doSignal(Node first) {
    do {
        // 更新首尾指针
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        // 帮助gc    
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&
             (first = firstWaiter) != null);
}
```

### transferForSignal

Transfers a node from a condition queue onto sync queue. 
Returns true if successfully transferred (else the node was cancelled before signal)

```text
final boolean transferForSignal(Node node) {
    /*
     * If cannot change waitStatus, the node has been cancelled.
     */
    if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
        return false;

    /*
     * Splice onto queue and try to set waitStatus of predecessor to indicate that thread is (probably) waiting. If cancelled or
     * attempt to set waitStatus fails, wake up to resync (in which case the waitStatus can be transiently and harmlessly wrong).
     */
    Node p = enq(node);
    int ws = p.waitStatus;
    if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL)) // 如果(ws >0) 意味着发生了等待节点取消(超时/中断); cas失败能想到的是发生了等待节点取消
        LockSupport.unpark(node.thread);
    return true;
}
```