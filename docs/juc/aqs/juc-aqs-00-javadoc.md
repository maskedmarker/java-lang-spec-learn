# juc-AQS-javadoc

CLH-lock是AQS的关键,所以先解释Node相关的javadoc.


## Node

```text
Wait queue node class.

The wait queue is a variant of a "CLH" (Craig, Landin, and Hagersten) lock queue. CLH locks are normally used for spinlocks. (CLH-lock中的线程都是在spin自旋轮询前节点的status实现等待.而AQS则通过挂起线程来等待,虽然节省了CPU开销却让线程因为挂起得不到CPU执行而无法实时感知前节点状态变化.)
We instead use them for blocking synchronizers, but use the same basic tactic of holding some of the control information about a thread in the predecessor of its node. 
A "status" field in each node keeps track of whether a thread should block. A node is signalled when its predecessor releases. (这里的signalled是一个泛指. 具体指的是unpark)
Each node of the queue otherwise serves as a specific-notification-style monitor holding a single waiting thread. (每个线程都紧盯先节点的状态,当尝试抢占锁失败而挂起等待,后被唤醒去重新尝试抢占锁)
The status field does NOT control whether threads are granted locks etc though. A thread may try to acquire if it is first in the queue. But being first does not guarantee success; it only gives the right to contend. So the currently released contender thread may need to rewait. (排在其他线程前面的线程才能去尝试抢占锁资源tryAcquire,排在后面的线程不能去尝试)

To enqueue into a CLH lock, you atomically splice it in as new tail. To dequeue, you just set the head field.
       +------+  prev +-----+       +-----+
  head |      | <---- |     | <---- |     |  tail
       +------+       +-----+       +-----+
  
Insertion into a CLH queue requires only a single atomic operation on "tail", so there is a simple atomic point of demarcation from unqueued to queued. (通过先设置prev后cas-tail再设置next以及借助循环重试完成并发下的无锁入队操作)
Similarly, dequeuing involves only updating the "head". However, it takes a bit more work for nodes to determine who their successors are, in part to deal with possible cancellation due to timeouts and interrupts.(由于支持节点超时/中断引发的取消操作,导致前节点无法准确确定其后节点是谁)
The "prev" links (not used in original CLH locks), are mainly needed to handle cancellation. (CLH-lock中使用隐式prev字段,AQS将其显性化.AQS的FIFO同步队列是由prev字段实现的且保证实时准确,可以安全可靠的通过tail+prev来从后向前遍历确定节点是否发生了取消操作)
If a node is cancelled, its successor is (normally) relinked to a non-cancelled predecessor. (relinked会发生在节点取消操作和准备挂起时)
For explanation of similar mechanics in the case of spin locks, see the papers by Scott and Scherer at http://www.cs.rochester.edu/u/scott/synchronization/ (链接已经失效了,可以参考https://github.com/urcs-sync/Queue-Locks-with-Timeout)

We also use "next" links to implement blocking mechanics. The thread id for each node is kept in its own node, so a predecessor signals the next node to wake up by traversing next link to determine which thread it is. 
Determination of successor must avoid races with newly queued nodes to set the "next" fields of their predecessors. 
This is solved when necessary by checking backwards from the atomically updated "tail" when a node's successor appears to be null. (Or, said differently, the next-links are an optimization so that we don't usually need a backward scan.)
(prev字段可以保证实时准确,想要获得next字段指向的对象都可以通过tail+prev反向遍历获得,所以没有保证next字段的实时准确性,只是尽力保证正确. next字段只是尽量避免通过tail+prev反向遍历的一种优化手段)

Cancellation introduces some conservatism to the basic algorithms. Since we must poll for cancellation of other nodes, we can miss noticing whether a cancelled node is ahead or behind us. 
This is dealt with by always unparking successors upon cancellation, allowing them to stabilize on a new predecessor, unless we can identify an uncancelled predecessor who will carry this responsibility.
(⚠️这一段是AQS的核心处理方案
CLH-lock中的线程都是在spin轮询前节点的status并非挂起的,所以能立马感知到前节点status的改变;而且前面节点也不支持超时/中断引发的取消操作.
如果让CLH-lock支持超时/中断引发的取消操作,仅仅需要改动一点点:将原来判断紧挨的前节点的status状态 改为 跳过取消的节点,判断最近的正常前驱节点的status状态.
AQS没有选择让线程spin忙等待而是挂起等待,虽然这样节省了cpu开销却让线程在挂起等待时缺失了感知前驱节点的status状态的能力.这就要求前驱节点在结束占用锁资源后通过结束后驱节点的挂起等待来恢复临时感知能力.
由于前驱节点唤醒后驱节点与该前驱节点的后驱节点的超时/中断引发的取消操作可能会同时发生,除非能够准确判断绝不会并发,否则就直接结束后面节点的挂起等待让其恢复感知能力,让其自己来决定.
备注: 
一种特别暴力简单的方法就是前驱节点唤醒后面所有的节点.节点的行为仅仅是因为挂起而暂停了,全部唤醒并不会改变节点线程既定行为逻辑.
"unless we can identify an uncancelled predecessor" 反倒是省cpu的一种优化术,而"always unparking successors"才是正确的大道.
)

CLH queues need a dummy header node to get started. 
But we don't create them on construction, because it would be wasted effort if there is never contention. Instead, the node is constructed and head and tail pointers are set upon first contention.
(锁竞争比较低时,tryAcquire的CAS就足够了,不会用到同步队列,延迟初始化同步队列是有用的)

Threads waiting on Conditions use the same nodes, but use an additional link. 
Conditions only need to link nodes in simple (non-concurrent) linked queues because they are only accessed when exclusively held. (至少在JDK的库中,Condition都是被互斥锁保护的.)
Upon await, a node is inserted into a condition queue. (在执行await方法时,为当前线程在条件队列中插入线程节点)
Upon signal, the node is transferred to the main queue. (在执行signal方法时,将条件队列中之前插入的线程节点转移到同步队列中)
A special value of status field is used to mark which queue a node is on. (只有节点在条件队列中时,node.waitStatus是Node.CANCELLED)
```

总结: 
1. 由于节点会挂起等待,整个唤醒机制严重依赖前面节点唤醒后面节点.
2. 由于支持节点超时/中断引发的取消操作,导致前节点无法准确确定该唤醒哪个后面节点(可能刚决定了一个后面节点,同时它发生了超时/中断引发的取消操)
3. CLH-lock中的节点就没有这些顾虑的根本原因是所有线程都没有挂起.所以AQS在面对可能存在的并发操作时直接unpark后面的节点线程,让其自旋来决定.(这是一种向CLH-lock中的节点线程自旋的退化)
4. AQS在面对可能存在的并发操作时只unpark后面一个节点线程而非全部,在能准确判断不存在并发的情况下还是选择unpark最近的后面节点.


## AbstractQueuedSynchronizer

```text
Provides a framework for implementing blocking locks and related synchronizers (semaphores, events, etc) that rely on first-in-first-out (FIFO) wait queues. (AQS的等待队列是通过prev构建的FIFO队列,等得久的线程先获得尝试抢占锁资源.)
This class is designed to be a useful basis for most kinds of synchronizers that rely on a single atomic int value to represent state. (锁的状态用一个int来表示,全部cpu都支持32-bit的cas原子操作指令)
Subclasses must define the protected methods that change this state, and which define what that state means in terms of this object being acquired or released. (子类要实现能改变state的tryAcquire/tryRelease方法,具体怎么改变基于子类怎么定义state含义)
Given these, the other methods in this class carry out all queuing and blocking mechanics. 
Subclasses can maintain other state fields, but only the atomically updated int value manipulated using methods getState, setState and compareAndSetState is tracked with respect to synchronization.


Subclasses should be defined as non-public internal helper classes that are used to implement the synchronization properties of their enclosing class. (AQS通常作为锁的内部类来使用,并不直接被锁继承.基于此AQS也没有实现Lock接口.)
Class AbstractQueuedSynchronizer does not implement any synchronization interface. 
Instead it defines methods such as acquireInterruptibly that can be invoked as appropriate by concrete locks and related synchronizers to implement their public methods.(作者希望AQS以组合而非继承的方式被使用)


This class supports either or both a default exclusive mode and a shared mode. (AQS支持独占模式和共享模式)
When acquired in exclusive mode, attempted acquires by other threads cannot succeed. Shared mode acquires by multiple threads may (but need not) succeed. 
This class does not "understand" these differences except in the mechanical sense that when a shared mode acquire succeeds, the next waiting thread (if one exists) must also determine whether it can acquire as well. 
Threads waiting in the different modes share the same FIFO queue. 
Usually, implementation subclasses support only one of these modes, but both can come into play for example in a ReadWriteLock. 
Subclasses that support only exclusive or only shared modes need not define the methods supporting the unused mode.


This class defines a nested AbstractQueuedSynchronizer.ConditionObject class that can be used as a Condition implementation by subclasses supporting exclusive mode (ConditionObject是提供给互斥锁使用的)
for which method isHeldExclusively reports whether synchronization is exclusively held with respect to the current thread, 
method release invoked with the current getState value fully releases this object, and acquire, given this saved state value, eventually restores this object to its previous acquired state. (因为ConditionObject是供互斥锁的,所以release时一定是将所有的锁资源都释放,即await要用fullRelease.那么为了恢复到await前的相同状态,await-reacquire-lock时要acquire到与刚才release同等数量的锁资源)
No AbstractQueuedSynchronizer method otherwise creates such a condition, so if this constraint cannot be met, do not use it. (如果是非互斥锁,请勿使用ConditionObject)
The behavior of AbstractQueuedSynchronizer.ConditionObject depends of course on the semantics of its synchronizer implementation.


This class provides inspection, instrumentation, and monitoring methods for the internal queue, as well as similar methods for condition objects. 
These can be exported as desired into classes using an AbstractQueuedSynchronizer for their synchronization mechanics.
```

## 接口API



```text
tryAcquire :: boolean
 返回值为true: 表示抢占锁成功
返回值为false: 表示抢占锁失败,本线程挂起等待


tryRelease :: boolean
 返回值为true: 表示锁资源释放成功(其他等待的线程可以去尝试抢占锁资源)  (由于是独占模式,锁资源释放成功也就意味着此时锁资源完全释放)
返回值为false: 表示锁资源释放失败(其他等待的线程保持继续等待)


tryAcquireShared :: int
返回值为正数:  表示抢占锁成功,且还有剩余的锁资源(其他等待的共享模式线程可以去尝试抢占锁资源)
  返回值为0:  表示抢占锁成功,且还没有剩余的锁资源(其他等待的线程保持继续等待)
返回值为负数:  表示抢占锁失败,本线程挂起等待(其他等待的线程保持继续等待)

tryReleaseShared :: boolean
 返回值为true: 表示锁资源释放成功(其他等待线程都可以去尝试抢占锁资源)
返回值为false: 表示锁资源释放失败(其他等待的线程保持继续等待)
```