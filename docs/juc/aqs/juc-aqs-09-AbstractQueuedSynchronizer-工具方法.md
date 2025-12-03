# juc-AQS-方法注释-AbstractQueuedSynchronizer


## 工具方法

### setHead

AQS的FIFO同步等待队列是
    1. 基于prev属性构成的链表的基础数据结构
    2. 通过移动head指针来控制队头节点出队
    3. 通过先设置node.prev:=tail再cas-tail来实现队尾入队

```text
private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.prev = null;
}
```

### addWaiter

一定能向同步队列尾部成功插入一个node.(不支持中断)

AQS的同步等待队列是通过Node.prev属性实现的FIFO的队列.Node.next属性是用来辅助优化算法的.
尝试抢占锁资源失败的线程在同步等待队列的队尾添加节点.

AQS的同步等待队列指的是头节点后的所有节点.头节点永远是一个dummy-node,用dummy-node.waitStatus来容纳后面节点是否需要被唤醒.

⚠️瞬时的临界问题需要上层方法注意并处理.


```text
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    
    // 先尝试enq的简化版(一次CAS将当前节点加入队尾)
    if (pred != null) { // 如果pred==null,意味着tail==null,此时同步队列还未初始化,需要用enq来完成
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node; // 先设置prev,cas完成设置tail后再设置next,保证同步prev队列(次优保证同步next队列)
            return node;
        }
    }
    // 失败了再尝试enq的完整版(通过循环来完成CAS操作,将当前节点加入队尾)
    enq(node);
    return node;
}


// 使用了优化的入队算法,通过先设置node.prev,再CAS tail指针,CAS成功后再设置node.prev.next值,保证了入队的线程安全; 通过CAS head保证初始化线程安全.
private Node enq(final Node node) {
    for (;;) {  // 通过不断循环一定要将node插入到队列的尾部
        Node t = tail;
        if (t == null) { // 队列未初始化
            if (compareAndSetHead(new Node())) // CAS先往队列中插入一个dummy head(队列中当前节点的waitStatus需要承载后续线程节点(不一定必须是紧挨的下个节点)需要被通知的信息,所以需要这样一个dummy-node)
                tail = head; // 注意: CAS先tail后head,会让其他线程看到:head!=null但tail==null,这里没有破坏Invariant:尾节点的next为null,头节点的prev为null
        } else { // 队列已经初始化
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}




注意同步队列在尾部添加节点时:
线程节点先设置prev再CAS设置tail指针,最后设置prev.next.
    在cas-tail指针前设置prev成功后,此时tail还是旧值,未来的新尾节点是不会被看到的.        (prev属性构成的链表是完整的,但next构成的链表是完整的)
    在cas-tail指针成功后到设置prev.next前是存在间隙的.                              (prev属性构成的链表是完整的,但next构成的链表不是完整的)
    在cas-tail指针后设置prev.next成功后.                                         (prev属性构成的链表是完整的,但next构成的链表是完整的)
初始化节点先设置head再设置tail
    cas-head保证了只有一个线程成功始化,其他线程只能通过else逻辑在头节点后依次追加
    先设置head再设置tail的操作会让其他线程看到:head!=null但tail==null                (prev属性构成的链表是完整的,但next构成的链表是完整的)


总结:
    addWaiter能保证一定将线程节点追加到同步队列队尾
    同时存在瞬时的临界问题
        同步等待队列在初始化时,其他线程会看到不一致的链表:head!=null但tail==null; 同步等待队列初始化后,追加线程节点会看到不一致的链表: 在cas-tail指针成功后到设置prev.next前是存在间隙的
        如上的问题,prev链表都保持了一致性,二next链表就无法保证一致性.
    临界问题应该有上层方法来处理,addWaiter及其子方法enq不应该处理💯
```


### hasQueuedPredecessors

Returns: true if there is a queued thread preceding the current thread, and false if the current thread is at the head of the queue or the queue is empty


⚠️本方法不考虑取消节点


```text
public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized before tail and on head.next being accurate if the current thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order (与队列初始化反方向读取,这样后面的h!=t才能正确表示队列中有线程节点)
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}

(h != t)成立时,可能同步队列正在初始化中(head!=null,head.next=null 且 tail==null, 此时head!=tail)也可能初始化完成后有线程节点加入
(h.next!=null)成立时,节点h不是尾节点;  (备注:利用了Invariant:只有尾节点的next是null)

(h != t && (s = h.next) == null)表示同步队列正在初始化中,即有线程抢占锁失败后正在入同步队列(肯定不是当前线程正在入队,当前线程无法做到同时执行当前方法又同时入队,如果要入队肯定在排在这个节点后面)
(h != t && (s = h.next) != null && s.thread != Thread.currentThread()) 表示初始化完成后有线程节点加入且第一个线程节点不是当前线程.



(h != t && ((s = h.next) == null || s.thread != Thread.currentThread()))  等价转为 !(h == t || ((s = h.next) != null && s.thread == Thread.currentThread())) 
(h == t || ((s = h.next) != null && s.thread == Thread.currentThread())) 表示同步队列未初始化或者初始后没有节点线程(the queue is empty)/或者当前线程是第一节点线程(at the head of the queue)
等价转换的语义更清晰
```


### isOnSyncQueue

入参的node初始位于条件队列中,判断此时该节点是否已经转移到同步队列中了.

Returns true if a node, always one that was initially placed on a condition queue, is now waiting to reacquire on sync queue.


isOnSyncQueue等价写法,逻辑更加明了
```text
final boolean isOnSyncQueue(Node node) {
    // 条件队列中的节点都是(.waitStatus == Node.CONDITION 且.prev == null) | <利用条件队列中节点的invariant来判断是否在条件队列中>
    if (!(node.waitStatus == Node.CONDITION && node.prev == null))
        return false;
    
    // 同步队列中的非尾节点的.next都是非null,此时node是中间节点    | <利用同步队列中间节点的invariant来判断是否在条件队列中>
    if (node.next != null)
        return true;
    
    // 其实完全可以直接遍历同步队列(即prev队列)来检查,前面的2个if都是优化写法
    return findNodeFromTail(node);
}
```

### transferAfterCancelledWait


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


