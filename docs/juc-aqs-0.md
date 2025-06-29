# juc-AQS-0

## AQS核心思想
```text
AQS使用一个volatile int类型的成员变量state来表示同步状态,通过内置的FIFO队列来完成资源获取线程的排队工作.

1. 核心数据结构
state: 同步状态,通过getState()、setState()和compareAndSetState()方法进行操作
CLH队列: 一个双向队列,用于存储等待线程
    CLH 队列最初用于 自旋锁（Spin Lock）,后来被 AQS 改进用于 阻塞式同步.
        无锁设计：使用 CAS（Compare-And-Swap）操作,减少线程阻塞
        公平性：严格 FIFO 顺序,避免线程饥饿.
        低开销：仅需少量原子变量维护队列.

2. 两种资源共享方式
独占模式(Exclusive): 一次只有一个线程能执行,如ReentrantLock
共享模式(Share): 多个线程可同时执行,如Semaphore/CountDownLatch
```

## AQS工作原理
```text
1. 获取资源流程

public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}

首先调用tryAcquire尝试获取资源(由子类实现)
如果获取失败,将当前线程包装为Node加入CLH队列
在队列中自旋尝试获取资源(挣扎一下),失败则挂起线程


2. 释放资源流程

public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}

调用tryRelease尝试释放资源(由子类实现)
唤醒后继节点中的线程
```

## 关键方法
```text
AQS采用了模板方法模式,需要子类实现以下方法：

独占模式:
tryAcquire(int arg): 尝试获取资源
tryRelease(int arg): 尝试释放资源

共享模式:
tryAcquireShared(int arg): 尝试获取共享资源
tryReleaseShared(int arg): 尝试释放共享资源
```

## AQS特点
```text
可重入性: 支持线程重复获取锁
公平性选择: 支持公平和非公平两种模式
可中断: 支持线程在等待过程中被中断
超时机制: 支持尝试获取锁的超时控制

AQS通过这种设计,极大地简化了同步器的实现,开发者只需关注state的获取和释放逻辑,而不需要处理复杂的线程排队、阻塞和唤醒机制.
```

AQS(AbstractQueuedSynchronizer)由volatile state和双向的FIFO链表构成;ConditionObject由单向链表构成.
前者可以称为同步队列(sync queue),后者可以称为条件队列(condition queue).

## AQS 的核心结构
```text
AQS 主要依赖以下组件：

state（同步状态）：一个 volatile int 变量，表示锁的状态（如 ReentrantLock 的持有计数）。
CLH 队列：一个双向 FIFO 队列，存储等待线程（Node 节点）。
CAS（Compare-And-Swap）：用于原子性修改 state 和队列节点。


1.1 CLH 队列（线程排队机制）
CLH 队列（Craig, Landin, and Hagersten lock queue）是一种 自旋锁优化(挣扎一下)后的 FIFO 队列，AQS 对其进行了改进：

节点（Node）：每个等待线程被封装成 Node，包含：
Thread thread：等待的线程
Node prev / Node next：前驱/后继节点
int waitStatus：节点状态（CANCELLED、SIGNAL、CONDITION、PROPAGATE）
头节点（head）和尾节点（tail）：head 是当前持有锁的线程，tail 指向最后一个等待线程。

1.2 state（同步状态）
独占模式（Exclusive）（如 ReentrantLock）：
    state = 0：锁未被占用
    state = 1：锁被占用
    state > 1：可重入锁（同一个线程多次获取锁）
共享模式（Shared）（如 Semaphore）：
    state 表示可用资源数（如 Semaphore(5) 初始 state=5）。
```

## AQS 的核心方法
```text
AQS 采用 模板方法模式，子类只需实现部分方法：

方法	作用	是否必须实现
tryAcquire(int)	        尝试获取独占锁	    ✔️（独占模式）
tryRelease(int)	        尝试释放独占锁	    ✔️（独占模式）
tryAcquireShared(int)	尝试获取共享锁	    ✔️（共享模式）
tryReleaseShared(int)	尝试释放共享锁	    ✔️（共享模式）
isHeldExclusively()	    当前线程是否独占锁	可选
```

## 方法说明
```text
void acquire(long arg)
    Acquires in exclusive mode, ignoring interrupts. 
    Implemented by invoking at least once tryAcquire, returning on success. Otherwise the thread is queued, possibly repeatedly blocking and unblocking, invoking tryAcquire until success. 
    This method can be used to implement method Lock.lock.

boolean tryAcquire(long arg)    
    Attempts to acquire in exclusive mode. 
    This method should query if the state of the object permits it to be acquired in the exclusive mode, and if so to acquire it.
    Returns: true if successful. Upon success, this object has been acquired.

boolean shouldParkAfterFailedAcquire(Node pred, Node node)
    Checks and updates status for a node that failed to acquire. Returns true if thread should block. This is the main signal control in all acquire loops. Requires that pred == node.prev.
    Returns: true if thread should block
    
boolean acquireQueued(final Node node, long arg)
    Acquires in exclusive uninterruptible mode for thread already in queue. Used by condition wait methods as well as acquire.
    Returns: true if interrupted while waiting
    
release(long arg)
    Releases in exclusive mode. 
    Implemented by unblocking one or more threads if tryRelease returns true. This method can be used to implement method Lock.unlock.
    Returns: the value returned from tryRelease    
    
tryRelease(long arg)
    Attempts to set the state to reflect a release in exclusive mode.This method is always invoked by the thread performing release.
    Returns: true if this object is now in a fully released state, so that any waiting threads may attempt to acquire; and false otherwise.    
    
boolean parkAndCheckInterrupt()    
    Convenience method to park and then check if interrupted
    Returns: true if interrupted
```

## 方法注释

### addWaiter

向队列插入node.
先cas尝试在尾部插入,如果失败的话,再自旋插入(确保一定能入队).
整体来看,就是个自旋插入.

```text
java.util.concurrent.locks.AbstractQueuedSynchronizer.addWaiter
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode); // 创建新节点
    Node pred = tail;
    if (pred != null) { // 队列非空,直接CAS插入尾部
        node.prev = pred;
        if (compareAndSetTail(pred, node)) { // CAS 更新 tail
            pred.next = node;
            return node;
        }
    }
    enq(node); // 队列为空或CAS失败,进入enq自旋插入
    return node;
}

java.util.concurrent.locks.AbstractQueuedSynchronizer.enq
private Node enq(final Node node) {
    for (;;) {  // 自旋
        Node t = tail;
        if (t == null) { // 初始化
            if (compareAndSetHead(new Node())) // 初始化时先插入一个dummy head, 同时表示head和tail
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) { // 先插入tail
                t.next = node;
                return t;
            }
        }
    }
}
```

```text
boolean acquireQueued(final Node node, long arg)
    Acquires in exclusive uninterruptible mode for thread already in queue. Used by condition wait methods as well as acquire.
    Returns: true if interrupted while waiting(只计算因为park导致waiting过程中发生的中断)
    
通过自旋获取锁资源.
为了防止cpu空转(避免忙等),只有head的下一位才被允许通过cas来争夺锁资源,
其他的node需要通过park将自己的线程挂起,等待被通知后才能恢复线程调度.

final boolean acquireQueued(final Node node, long arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) { // 自旋
            final Node p = node.predecessor();
            // 只有获取到锁资源,才能退出自旋(外带,同时将当前node设置为head)
            // 下面的if过程,线程没有park即非waiting,所以直接忽略interrupted状态位
            if (p == head && tryAcquire(arg)) { // 前驱是head时才尝试获取锁,其他场景就自选等待
                setHead(node); // 获取成功,当前节点成为head
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            // 因为cas争夺锁失败(可能是非公平竞争导致),将前驱节点的waitStatus设置为SIGNAL,即通知前驱节点释放锁资源时唤醒自己,然后才能再次进入自旋
            if (shouldParkAfterFailedAcquire(p, node) && // 检查是否需要挂起
                parkAndCheckInterrupt()) // 挂起线程,且线程恢复调度时,检查是否是因为interrupt导致退出park方法
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

```text
boolean shouldParkAfterFailedAcquire(Node pred, Node node)
    Checks and updates status for a node that failed to acquire. Returns true if thread should block. 
    This is the main signal control in all acquire loops. 
    Requires that pred == node.prev.
    Params:
        pred – node's predecessor holding status 
        node – the node
    Returns: true if thread should block

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
```

```text
public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized before tail and on head.next being accurate if the current thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}

2.1 变量读取顺序
为什么逆序读取？
避免与 enq() 方法初始化队列时的顺序冲突（AQS 初始化队列时先设置 head，再设置 tail）
保证可见性：volatile 写操作是 head → tail，逆序读取能感知到最新的 tail

2.2 核心判断逻辑
return h != t && 
       ((s = h.next) == null || s.thread != Thread.currentThread());

条件1：h != t
    true：队列不为空（至少有一个等待节点）
    false：队列为空（head == tail），直接返回 false（无竞争）

条件2：(s = h.next) == null
    true：极端并发情况下，head 已更新但 head.next 还未链式更新（非常短暂的状态）
          此时保守认为有其他线程正在竞争，返回 true
    false：正常情况，继续检查下一个条件

条件3：s.thread != Thread.currentThread()
    true：(同步队列中的head是虚拟节点,h.next才是有意义的线程节点)head.next 的线程不是当前线程，说明有其他线程更早排队
    false：当前线程就是 head.next 的持有者（可尝试获取锁）       
```

