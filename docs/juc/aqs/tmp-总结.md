# juc-AQS-总结

## 临时记录

```text
waitStatus 

当前线程需要其他线程唤醒则是在前驱节点(并非紧挨的前一个节点)的waitStatus上设置SIGNAL;
当前线程在acquire中发生异常需要取消时,则是在自己节点(而非前驱节点)上设置CANCELED;



PROPAGATE是主动设置要向后传递信息,
waitStatus初始值就是0,还没来得及设置,未来防止发生missed-signal,应该向后传递信息

signal是在前驱节点上设置,而cancel是在自己节点上设置

waitStatus初始为0,没有被设置任何信息,就是一个普通节点(正常处于同步状态)
waitStatus何时再次为0



按照CLH-lock中锁状态的多副本理解, SIGNAL指的是多面有节点需要唤醒,不是next节点需要唤醒.
```

```text
state: AQS maintains a simple int value (state) to represent the synchronization state. 
    The meaning of state varies depending on the synchronizer you create (e.g., lock held count for a reentrant lock, permits for a semaphore, etc.).
Exclusive Mode: 
    Only one thread can hold the resource (e.g., ReentrantLock).
Shared Mode: 
    Multiple threads can share the resource (e.g., Semaphore, CountDownLatch).
```

```text
Your custom synchronizer class will generally:

Extend AbstractQueuedSynchronizer.
Use the state variable to model your synchronization logic.
Implement key methods such as tryAcquire, tryRelease, tryAcquireShared, and tryReleaseShared based on whether you’re implementing exclusive or shared behavior.


Implement Required Methods
For Exclusive Mode:
Override:
    tryAcquire(int arg): Define the logic to acquire the resource exclusively. Return true if the acquisition is successful, otherwise return false.
    tryRelease(int arg): Define the logic to release the resource. Return true if the state transition occurs and allows waiting threads to proceed.

For Shared Mode:
Override:
    tryAcquireShared(int arg): Define the logic to acquire the resource in shared mode. Return:
        Negative if the acquisition fails.
        Zero if no more shared acquisitions are allowed.
        Positive if the acquisition is successful, and more threads can share the resource.
    tryReleaseShared(int arg): Define the logic to release the resource in shared mode. Usually, decrement the state and decide if more threads can proceed.
```


```text
tryRelease 只能抛出
IllegalMonitorStateException – if releasing would place this synchronizer in an illegal state. This exception must be thrown in a consistent fashion for synchronization to work correctly.
UnsupportedOperationException – if exclusive mode is not supported
```

```text
Doug Lea在<<The java.util.concurrent synchronizer framework>>说:

The main complication in implementing these operations is dealing with cancellation of condition waits due to timeouts or Thread.interrupt. 
A cancellation and signal occurring at approximately the same time encounter a race whose outcome conforms to the specifications for built-in monitors. 
As revised in JSR133, these require that if an interrupt occurs before a signal, then the await method must, after re-acquiring the lock, throw InterruptedException. 
But if it is interrupted after a signal, then the method must return without throwing an exception, but with its thread interrupt status set.

To maintain proper ordering, a bit in the queue node status records whether the node has been (or is in the process of being) transferred. 
Both the signalling code and the cancelling code try to compareAndSet this status. 
⚠️If a signal operation loses this race, it instead transfers the next node on the queue, if one exists. 
⚠️If a cancellation loses, it must abort the transfer, and then await lock re-acquisition.
```

```text
AQS的acquire类方法和release类方法都会引起AQS的状态变化,但最主要的不同体现在
acquire类方法可能会将线程挂起等待,而release类方法永远不会挂起线程.
```

```text
AQS的同步队列指的是Node.prev组成的FIFO队列(Node.next只是辅助字段)
Condition的等待队列指的是Node.nextWaiter组成的FIFO队列(用lastWaiter指针enq入队,用firstWaiter指针deq出队)
```

```text
取消(cancellation)

取消分2类: 
    在同步队列中等待时,取消等待,即取消同步等待;
    在条件队列中等待时,取消等待,即取消条件等待;

发生取消的原因分2类:
    因中断导致退出park,线程恢复调度;
    因超时导致退出park,线程恢复调度;
```


```text
在原始版本的 CLH 锁中，节点间甚至都没有互相链接。但是，通过在节点中显式地维护前驱节点，CLH 锁就可以处理“超时”和各种形式的“取消”：如果一个节点的前驱节点取消了，这个节点就可以滑动去使用前面一个节点的状态字段。对于通过自旋获取锁的 CLH 锁来说，只需要显式的维护前驱节点就可以实现取消功能.
但是在 AQS 的实现稍有不同。因为 AQS 用阻塞等待替换了自旋操作，线程会阻塞等待锁的释放，不能主动感知到前驱节点状态变化的信息。AQS 中显式的维护前驱节点和后继节点，需要释放锁的节点会显式通知下一个节点解除阻塞

⚠️⚠️⚠️⚠️恢复感知到前驱节点状态变化
因为 AQS 用阻塞等待替换了自旋操作，线程会阻塞等待锁的释放，不能主动感知到前驱节点状态变化的信息,所以需要释放锁的节点通过unpark下一个节点线程使其恢复恢复感知到前驱节点状态变化(即恢复调度).
又因为最前的节点线程先抢占,所以下一个节点要选取队列最前的.
```


```text
🚀怎么证明锁本身是正确的？

核心思想：构造一个无可辩驳的论证
证明锁的正确性，本质上是构造一个逻辑论证，说明在所有可能的程序执行交错下，锁的属性都成立。由于并发执行的可能性是无穷的，我们需要一种抽象和归纳的方法。

证明框架
形式化定义：用精确的、数学化的语言定义“锁”、“持有锁”、“互斥”等概念。
识别不变量：找出一个或多个在锁算法执行过程中始终为真的条件。这是最关键的一步。💯
基于不变量进行证明：
    互斥：证明锁的不变量蕴含着“不可能有两个线程同时持有锁”。
    无死锁/无饥饿：证明任何一个试图获取锁的线程，最终都能成功。这通常需要证明存在一个“先后顺序”或“等待链”，并且这个顺序是有限的。
    
    
证明锁本身的正确性是一个严谨的、逻辑驱动的过程：
    1. 对于简单锁（如Peterson锁）：可以通过识别不变量和反证法来手工构造一个令人信服的证明。
    2. 对于复杂锁（如队列锁）：利用其显式维护的顺序来简化证明。
    3. 对于工业级、性命攸关的锁：必须依赖形式化验证工具进行机器辅助的、穷尽的检查。
这个过程的本质是，将并发程序中不确定的交错执行，通过逻辑和数学的方法，转化为一个确定的、可推理的论证。    
```