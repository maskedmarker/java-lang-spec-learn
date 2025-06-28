# juc-AQS

AQS(AbstractQueuedSynchronizer)由volatile state和双向的FIFO链表构成;ConditionObject由单向链表构成.
前者可以称为同步队列(sync queue),后者可以称为条件队列(condition queue).


## Node

1. Node需要支持独占模式和共享模式(即排他锁场景和共享锁场景)
2. Node不仅用于AQS的FIFO链表,还被用于ConditionObject的单向链表.

### Node.waitStatus

Node.waitStatus表示的状态也要满足独占模式和共享模式

后面节点插入FIFO队列时,根据需要设置前节点的waitStatus
也有当前线程放弃时,设置自己节点的waitStatus(???未确认)

```text
AbstractQueuedSynchronizer.Node.waitStatus 是一个非常关键的字段,用于描述队列中每个节点(线程)当前的等待状态.它用于协调线程的阻塞、唤醒和取消逻辑.

waitStatus 的取值与含义
static final int CANCELLED  =  1;
static final int SIGNAL     = -1;
static final int CONDITION  = -2;
static final int PROPAGATE  = -3;


1. CANCELLED = 1
含义：
该线程已取消等待(比如被中断或超时),该节点不会再被调度执行.

特点：
节点会从队列中“逻辑上剔除”,不会被唤醒.
后继节点会跳过它,寻找前一个有效节点.
一旦进入 CANCELLED 状态,waitStatus 不会再被更改.

2. SIGNAL = -1
含义：
后继节点需要被唤醒(即当前节点退出时,需要唤醒下一个节点).

特点：
是最常见的状态.
表示前驱节点正在(或即将)阻塞,当前节点需要等前驱释放后唤醒自己.

例子：
某线程在获取锁失败后进入等待队列,它的前一个节点的 waitStatus 会被设为 SIGNAL.

3. CONDITION = -2
含义：
节点在 Condition 条件队列中等待(不是在主同步队列中).

特点：
此状态仅出现在 ConditionObject 中(如 await() 时).
等到 signal() 被调用时,节点才会被移动到 AQS 的同步队列中.

4. PROPAGATE = -3
含义：
共享模式下传播唤醒的标志,表示需要继续唤醒后续节点.

特点：
用于如 CountDownLatch, Semaphore 等共享同步器.
head 节点在释放共享资源时设置它,以表明需要继续唤醒后继节点.

5. 0(默认状态)
含义：
无特殊含义,表示正常状态.

说明：
初始为 0.
一般是在未进入等待状态前,或状态被清除后.
```

### 状态转换关系图(简化)
```text

       tryAcquire fail
Thread -----------------> Node(SIGNAL)
                              |
                              | 被 unpark 后
                              v
                          tryAcquire success
                              |
                              v
                            head
                            
                            
在 Condition.await() 中的转换：

await() --> Node(CONDITION) --> 被 signal()
                                 |
                                 v
                           移入同步队列并变为 SIGNAL                            
```



## Condition

```text
java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject.await()

public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    Node node = addConditionWaiter();
    int savedState = fullyRelease(node);
    int interruptMode = 0;
    while (!isOnSyncQueue(node)) {
        // 将当前线程挂起,除非由其他线程将当前线程的node重新放入到AQS的acquire队列中
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