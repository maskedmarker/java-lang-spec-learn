# juc-AQS-总结

AQS的state用来表示锁资源,FIFO队列用来处理排队等待的线程.
acquire类方法被设计为可能会导致调用方挂起等待;而release类线程则不会导致调用方挂起等待.
Condition是为独占模式提供的,共享模式不能使用.

## FIFO队列

```text
AQS的同步队列和条件队列,都使用了相同的数据结构: 链表+首尾指针
入队时,在尾指针指向的链表节点后添加节点,然后将尾指针指向新增的节点;
出队时,将头指针移动到头指针指向的链表节点的下一个节点.

具体实现时,AQS的同步队列和条件队列略有不同:
AQS的同步队列: Node.prev构成链表,首尾指针是head/tail
AQS的条件队列: Node.nextWaiter构成链表,首尾指针是firstWaiter/lastWaiter


备注:
1. Node.next属性是用来尽量避免通过tail+prev反向遍历的一种优化手段.
2. 代码中特别注意对prev字段的改动,保证了prev队列时时刻刻都准确;而next字段就不那么可靠了.
3. 入队操作使用优化算法: 先设置新节点node.prev,再cas-tail,最后设置的node.prev.next的操作不会不会发生racing问题.
```


```text
⚠️AQS要解决的核心问题:
1. CLH-lock中的线程都是一直主动spin自旋轮询前节点的status,所以能立马感知到前节点status的改变;而且节点也不支持超时/中断等引发的取消操作.整个算法可以放心地依靠前节点释放锁的操作必定能被后节点感知到.
2. 如果让CLH-lock支持超时/中断引发的取消操作,仅仅需要改动一点点:将原来判断紧挨的前节点的status状态 改为 跳过取消的节点,判断最近的正常前驱节点的status状态.
3. AQS基于CLH-lock算法,并在此基础之上新增了支持超时/中断引发的取消等待,并支持线程通过挂起/唤醒线程机制来等待(而非自旋忙等待).
4. AQS没有选择让线程spin忙等待而是挂起等待,虽然这样节省了cpu开销却让线程在挂起等待时缺失了感知前驱节点的status状态改变的能力.这就要求前驱节点在结束占用锁资源后通过结束后驱节点的挂起等待来恢复感知能力.
5. ⚠️AQS由于node节点唤醒node.next节点的操作与node.next节点的超时/中断引发的取消操作可能会同时发生,存在并发竟态问题.导致前节点无法准确确定该唤醒哪个后面节点(可能刚决定了一个后面节点,它却同时发生了超时/中断引发的取消操)
6. ⚠️解决这种存在的并发问题的暴力简单思路是模仿CLH-lock,让前驱节点唤醒后面所有的节点.
7. AQS选择了更加精细的操作,唤醒一个或者部分节点线程.
```


```text
🚀🚀🚀🚀要先理解AQS的acquire类操作,release类操作是基于acquire类操作的.
```

## 独占模式
```text
nothing
```

### tryAcquire/tryRelease
```text
tryAcquire/tryRelease 只能抛出
IllegalMonitorStateException – if releasing would place this synchronizer in an illegal state. This exception must be thrown in a consistent fashion for synchronization to work correctly.
UnsupportedOperationException – if exclusive mode is not supported

tryAcquire-Returns:true if successful. Upon success, this object has been acquired.
tryRelease-Returns:true if this object is now in a fully released state, so that any waiting threads may attempt to acquire; and false otherwise.
```


## 共享模式

💯💯💯💯💯💯💯💯💯💯💯💯 共享模式的最大不同就是级联唤醒

```text
在共享模式下,还是只有第一线程节点能才有尝试抢占锁资源的权力.
⚠️第一线程节点抢占到锁资源后,还要再连带唤醒一个第二节点线程.
⚠️第二节点线程在锁资源充足的情况下还会引发连带唤醒一个第三节点线程,以此类推.
⚠️这个连带唤醒在锁资源耗尽后就停止了,这样避免了锁资源还有而线程未被唤醒的场景.
```

### tryAcquireShared/tryReleaseShared
```text
只能抛出IllegalMonitorStateException/UnsupportedOperationException

tryAcquireShared-return: in shared mode, a negative value on failure; zero if acquisition but no subsequent acquire can succeed; and a positive value if acquisition and subsequent acquires might also succeed.
tryReleaseShared-return: true if this release of shared mode may permit a waiting acquire (shared or exclusive) to succeed; and false otherwise
```



## waitStatus

```text
// 当前线程因为超时或者中断被取消.这是一个终结态,也就是状态到此为止
static final int CANCELLED =  1;

// 当前线程的后继线程被阻塞或者即将被阻塞,当前线程释放锁或者取消后需要唤醒后继线程.这个状态一般都是后继线程来设置前驱节点的
static final int SIGNAL    = -1;

// 当前线程在condition队列中
static final int CONDITION = -2;

// 用于将唤醒后继线程传递下去,这个状态的引入是为了完善和增强共享锁的唤醒机制.在一个节点成为头节点之前,是不会跃迁为此状态的
static final int PROPAGATE = -3;

// 0 表示无状态
        
Status field, taking on only the values: 
SIGNAL: 
        The successor of this node is (or will soon be) blocked (via park), so the current node must unpark its successor when it releases or cancels. 
        To avoid races, acquire methods must first indicate they need a signal, then retry the atomic acquire, and then, on failure, block. 
CANCELLED: 
        This node is cancelled due to timeout or interrupt. Nodes never leave this state. In particular, a thread with cancelled node never again blocks. 
CONDITION: 
        This node is currently on a condition queue. 
        It will not be used as a sync queue node until transferred, at which time the status will be set to 0. (Use of this value here has nothing to do with the other uses of the field, but simplifies mechanics.) 
PROPAGATE: 
        A releaseShared should be propagated to other nodes. This is set (for head node only) in doReleaseShared to ensure propagation continues, even if other operations have since intervened. 
0: 
        None of the above The values are arranged numerically to simplify use. 

Non-negative values mean that a node doesn't need to signal. So, most code doesn't need to check for particular values, just for sign. 
The field is initialized to 0 for normal sync nodes, and CONDITION for condition nodes. 
It is modified using CAS (or when possible, unconditional volatile writes).
```