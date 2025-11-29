# juc-AQS-0

```text
AbstractQueuedSynchronizer

Provides a framework for implementing blocking locks(阻塞式锁) and related synchronizers (semaphores, events, etc) that rely on first-in-first-out  wait queues(基于FIFO等待队列).
This class is designed to be a useful basis for most kinds of synchronizers that rely on a single atomic int value to represent state(基于一个原子整数来表示状态). 
Subclasses must define the protected methods that change this state(子类要实现更改状态的方法), and which define what that state means in terms of this object being acquired or released(子类要自己定义state的含义). 
Given these, the other methods in this class carry out all queuing and blocking mechanics. (只有实现了如上的内容,aqs的其他方法实现排队和阻塞机制)
Subclasses can maintain other state fields, but only the atomically updated int value manipulated using methods getState, setState and compareAndSetState is tracked with respect to synchronization.(只有这3个方法)

Subclasses should be defined as non-public internal helper classes that are used to implement the synchronization properties of their enclosing class. 
Class AbstractQueuedSynchronizer does not implement any synchronization interface. 
Instead it defines methods such as acquireInterruptibly that can be invoked as appropriate by concrete locks and related synchronizers to implement their public methods.

This class supports either or both a default exclusive mode and a shared mode. 
When acquired in exclusive mode, attempted acquires by other threads cannot succeed. 
Shared mode acquires by multiple threads may (but need not) succeed. 
This class does not "understand" these differences except in the mechanical sense that when a shared mode acquire succeeds, the next waiting thread (if one exists) must also determine whether it can acquire as well. 
Threads waiting in the different modes share the same FIFO queue. 
Usually, implementation subclasses support only one of these modes, but both can come into play for example in a ReadWriteLock. 
Subclasses that support only exclusive or only shared modes need not define the methods supporting the unused mode.

中文:
提供了一个用于实现阻塞锁和相关同步器（如信号量、事件等）的框架，这些同步器都依赖于先进先出 (FIFO) 的等待队列。
这个类被设计为大多数同步器的有用基础，这些同步器通常依靠一个 单一的原子 int 值 来表示其内部状态。
子类必须定义受保护的（protected）方法，用于修改这个状态，并定义该状态在“同步器被获取或释放”时所代表的意义。
一旦这些方法被定义，AbstractQueuedSynchronizer 中的其他方法就会自动完成队列管理与阻塞控制的细节。
子类可以维护额外的状态字段，但只有通过 getState、setState、compareAndSetState 这几个方法操作的那个原子整数值，会被 AQS 框架视为真正的“同步状态”并参与同步逻辑。

子类通常应该被定义为非公开的内部帮助类（helper class），用于实现其外部类的同步功能。(如ReentrantLock的内部类Sync是AbstractQueuedSynchronizer,而ReentrantLock并不会直接继承AbstractQueuedSynchronizer)
AbstractQueuedSynchronizer 本身不直接实现任何同步接口。
相反，它定义了一些方法（例如 acquireInterruptibly），供具体的锁或同步器（例如 ReentrantLock、CountDownLatch 等）在其公开方法中调用，以实现所需的同步行为。

这个类支持两种（或同时支持两种）操作模式：
独占模式（exclusive mode） 和 共享模式（shared mode）。
    当一个线程以独占模式获取同步状态时，其他线程的获取尝试都会失败。
    在共享模式下，多个线程可以同时成功获取（例如读锁），但这不是强制要求的。
AQS 本身并不理解这些模式的语义差异；它只在“机械层面”上区分它们：
    当一个线程以共享模式成功获取时，下一个等待的线程（如果存在）也需要检查自己是否能继续获取。
不论是独占模式还是共享模式，所有线程都共用同一个 FIFO 等待队列。
通常，一个具体的同步器子类只支持其中一种模式，但某些同步器（例如 ReadWriteLock）会同时使用两种模式。
对于只支持单一模式的子类，不必实现与未使用模式相关的方法。
```

```text
Usage
To use this class as the basis of a synchronizer, redefine the following methods, as applicable, by inspecting and/or modifying the synchronization state using getState, setState and/or compareAndSetState:
tryAcquire
tryRelease
tryAcquireShared
tryReleaseShared
isHeldExclusively

Each of these methods by default throws UnsupportedOperationException. 
Implementations of these methods must be internally thread-safe, and should in general be short and not block. (这些方法在实现的时候一定是线程安全的,短小精悍,且非阻塞)
Defining these methods is the only supported means of using this class. All other methods are declared final because they cannot be independently varied.(只有这几个方法是提供给子类实现的,其他方法都是被声明为final,不允许改动的)
You may also find the inherited methods from AbstractOwnableSynchronizer useful to keep track of the thread owning an exclusive synchronizer. 
You are encouraged to use them -- this enables monitoring and diagnostic tools to assist users in determining which threads hold locks.


Even though this class is based on an internal FIFO queue, it does not automatically enforce FIFO acquisition policies. The core of exclusive synchronization takes the form:
  Acquire:
      while (!tryAcquire(arg)) {
         enqueue thread if it is not already queued;
         possibly block current thread;
      }
 
  Release:
      if (tryRelease(arg))
         unblock the first queued thread;
  
(Shared mode is similar but may involve cascading signals.)
Because checks in acquire are invoked before enqueuing, a newly acquiring thread may barge ahead of others that are blocked and queued. 
However, you can, if desired, define tryAcquire and/or tryAcquireShared to disable barging(抢占) by internally invoking one or more of the inspection methods, thereby providing a fair FIFO acquisition order. 
In particular, most fair synchronizers can define tryAcquire to return false if hasQueuedPredecessors (a method specifically designed to be used by fair synchronizers) returns true. 
Other variations are possible.
```

```text
Throughput and scalability are generally highest for the default barging (also known as greedy, renouncement, and convoy-avoidance) strategy. 
While this is not guaranteed to be fair or starvation-free, earlier queued threads are allowed to recontend before later queued threads, and each recontention has an unbiased chance to succeed against incoming threads. 
Also, while acquires do not "spin" in the usual sense, they may perform multiple invocations of tryAcquire interspersed with other computations before blocking. 
This gives most of the benefits of spins when exclusive synchronization is only briefly held, without most of the liabilities when it isn't.
If so desired, you can augment this by preceding calls to acquire methods with "fast-path" checks, possibly prechecking hasContended and/or hasQueuedThreads to only do so if the synchronizer is likely not to be contended.
This class provides an efficient and scalable basis for synchronization in part by specializing its range of use to synchronizers that can rely on int state, acquire, and release parameters, and an internal FIFO wait queue. 
When this does not suffice, you can build synchronizers from a lower level using atomic classes, your own custom java.util.Queue classes, and LockSupport blocking support.

默认的 barging（抢占）策略（也被称为贪婪策略（greedy）、放弃重排（renouncement） 或 避免“车队效应”（convoy-avoidance））通常在吞吐量和可扩展性方面表现最佳。
这种策略虽然不能保证公平性或避免线程饥饿，但它允许队列中较早的线程在比后来的线程更早地重新竞争锁。每次重新竞争时，它都有一个公平（无偏）的机会去与新来的线程竞争成功。
另外，线程在尝试获取锁（acquire）时，虽然不会像传统意义上那样“自旋”（spin），但它可能会多次调用 tryAcquire（尝试获取锁），并在这些尝试之间穿插执行其他计算，直到最终阻塞。
这种做法在锁被短时间持有的情况下，能获得类似“自旋锁”的性能优势，而又避免了长时间自旋造成的资源浪费。
如果需要，还可以通过在调用 acquire() 方法前加入所谓的“快速路径”检查（fast-path check）来优化，例如预先检查 hasContended() 和/或 hasQueuedThreads()，以便仅在锁可能未被竞争时才尝试快速获取**。
```



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
独占模式(Exclusive): 只有一个线程能执行,如ReentrantLock
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

### waitStatus

```text
// waitStatus value to indicate thread has cancelled
static final int CANCELLED =  1;

/** waitStatus value to indicate successor's thread needs unparking */
static final int SIGNAL    = -1;

/** waitStatus value to indicate thread is waiting on condition */
static final int CONDITION = -2;

//waitStatus value to indicate the next acquireShared should unconditionally propagate
static final int PROPAGATE = -3;
        
Status field, taking on only the values: 
SIGNAL: The successor of this node is (or will soon be) blocked (via park), so the current node must unpark its successor when it releases or cancels. To avoid races, acquire methods must first indicate they need a signal, then retry the atomic acquire, and then, on failure, block. 
CANCELLED: This node is cancelled due to timeout or interrupt. Nodes never leave this state. In particular, a thread with cancelled node never again blocks. 
CONDITION: This node is currently on a condition queue. It will not be used as a sync queue node until transferred, at which time the status will be set to 0. (Use of this value here has nothing to do with the other uses of the field, but simplifies mechanics.) 
PROPAGATE: A releaseShared should be propagated to other nodes. This is set (for head node only) in doReleaseShared to ensure propagation continues, even if other operations have since intervened. 
0: None of the above The values are arranged numerically to simplify use. 
Non-negative values mean that a node doesn't need to signal. So, most code doesn't need to check for particular values, just for sign. 
The field is initialized to 0 for normal sync nodes, and CONDITION for condition nodes. 
It is modified using CAS (or when possible, unconditional volatile writes).
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


