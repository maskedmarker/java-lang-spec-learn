# juc-AQS-1

## Node

1. Node需要支持独占模式和共享模式(即排他锁场景和共享锁场景)
2. Node不仅用于AQS的FIFO链表,还被用于ConditionObject的单向链表.

### Node.waitStatus

Node.waitStatus表示的状态也要满足独占模式和共享模式

后面节点插入FIFO队列时,根据需要设置前节点的waitStatus
也有当前线程放弃时,设置自己节点的waitStatus(???未确认)

同步队列中,Node.waitStatus没有CONDITION
条件队列中,Node.waitStatus没有SIGNAL(???)
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




## Node.PROPAGATE 状态的深入解析

在 AQS (AbstractQueuedSynchronizer)的Node类中,waitStatus 的 PROPAGATE 状态是一个容易被忽视但非常重要的设计,主要用于共享模式下的同步状态传播.它的作用是优化多线程并发释放资源时的唤醒效率.

1. PROPAGATE 状态的定义
```text
PROPAGATE 的值为 -3(源码中定义为 static final int PROPAGATE = -3),仅在 共享模式(如 Semaphore、CountDownLatch)下使用.它的核心作用是：
当共享资源被释放时,通知后续节点继续唤醒更多线程,避免“唤醒丢失”问题.
```

2. 为什么需要 PROPAGATE？
```text
问题背景
在共享模式下(如 Semaphore.release()),当一个线程释放资源时,它会唤醒后续等待的线程.但如果多个线程同时释放资源,可能会出现：

资源剩余但未被充分使用：由于并发释放,某些唤醒操作可能被覆盖.
线程饥饿：部分线程因未被及时唤醒而长时间等待.

PROPAGATE 的解决方案
通过将节点的 waitStatus 标记为 PROPAGATE,AQS会强制传播唤醒信号,确保所有可用资源被充分利用.
```

3. PROPAGATE 的工作机制

3.1 关键代码(setHeadAndPropagate)
```text
private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head; // 记录旧头节点
    setHead(node);  // 设置新头节点
    
    // 检查是否需要传播唤醒
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared(); // 传播唤醒
    }
}

逻辑解析：
如果 propagate > 0(还有剩余资源),或前驱节点的 waitStatus 是 SIGNAL/PROPAGATE,则继续唤醒后续节点.
调用 doReleaseShared() 传播唤醒信号.
```

3.2 doReleaseShared() 的传播逻辑
```text
private void doReleaseShared() {
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue; // CAS 失败重试
                unparkSuccessor(h); // 唤醒后继节点
            }
            else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue; // 标记为 PROPAGATE
        }
        if (h == head) // 如果 head 未变化,退出循环
            break;
    }
}

关键点：
如果头节点的 waitStatus 是 SIGNAL,则唤醒后继节点.
如果 waitStatus 是 0,则 CAS 改为 PROPAGATE,表示 后续释放操作需要传播唤醒.
```

4. 实际场景分析
```text
场景：Semaphore 的并发释放
线程A 释放资源,调用 release() -> doReleaseShared().
        将 head.waitStatus 从 SIGNAL 改为 0,并唤醒线程B.
线程B 被唤醒,获取资源后调用 setHeadAndPropagate.
        发现旧 head.waitStatus 是 0,CAS 改为 PROPAGATE.
线程C 同时释放资源,检查到 head.waitStatus 是 PROPAGATE,直接继续唤醒线程D.

如果没有 PROPAGATE：
        线程C 可能认为“已经有人处理唤醒”,从而不执行唤醒操作,导致线程D饥饿.
```

5. PROPAGATE 与其他状态对比
```text
waitStatus值	    状态	          用途
0                   初始状态	      新创建的节点
-1 (SIGNAL)	        需唤醒后继	  表示当前节点释放后需唤醒下一个节点
-2 (CONDITION)	    条件等待	      用于 ConditionObject 条件队列
-3 (PROPAGATE)	    传播唤醒	      共享模式下强制传播唤醒信号
1  (CANCELLED)	    已取消	      线程因超时或中断放弃等待
```

6. 总结
```text
PROPAGATE 的作用：解决共享模式下多线程并发释放资源时的“唤醒丢失”问题,确保资源被充分利用.

触发条件：
    共享模式下(如 Semaphore、CountDownLatch).
    在 setHeadAndPropagate 中检测到需要传播唤醒.

核心逻辑：通过 CAS 将 head.waitStatus 标记为 PROPAGATE,强制 doReleaseShared() 继续唤醒后续线程.
```


## Node.waitStatus = 0 状态的深入解析

在 Java AQS (AbstractQueuedSynchronizer) 的 CLH 队列中,Node.waitStatus 是节点状态的核心标志位.当 waitStatus = 0 时,表示该节点处于 初始状态 或 无特殊状态,具体含义取决于上下文场景.

1. waitStatus = 0 的基本含义
```text
0 是 Node.waitStatus 的 默认初始值,表示：
    该节点 尚未被赋予任何特殊状态(如不需要唤醒后继节点、未进入条件队列等).
    它是一个 中立状态,可能后续会被修改为其他状态(如 SIGNAL、CANCELLED 等).
```

2. waitStatus = 0 的具体场景

场景 1：新创建的节点
```text
当一个线程首次加入 CLH 队列时,其 Node 的 waitStatus 初始化为 0：

Node(Thread thread, Node mode) { // Node 的构造函数
    this.nextWaiter = mode;
    this.thread = thread;
    this.waitStatus = 0; // 初始状态
}

说明：此时节点还未被赋予任何职责(如唤醒后继节点).
```

场景 2：节点已完成状态(临时过渡)
```text
在 AQS 的运行过程中,waitStatus 可能会从其他状态(如 SIGNAL)被 CAS 修改为 0,表示：
    该节点已处理完自己的职责(如已唤醒后继节点).
    后续可能被重新赋值为其他状态(如 PROPAGATE).

典型代码(doReleaseShared 方法片段)：

private void doReleaseShared() {
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                // 将 head 的 SIGNAL 状态改为 0,表示已处理唤醒
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue; // CAS 失败则重试
                unparkSuccessor(h); // 唤醒后继节点
            }
            // 如果 ws == 0,可能尝试改为 PROPAGATE
            else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;
        }
        // ...
    }
}

关键点：
当 head 节点的状态从 SIGNAL 改为 0 时,表示“已触发唤醒操作”.
如果此时其他线程并发释放资源,可能会将 0 改为 PROPAGATE(共享模式下).
```

场景 3：节点被取消(CANCELLED)后的清理
```text
当线程因超时或中断放弃等待时,节点的 waitStatus 会被改为 CANCELLED(1).在清理过程中,0 可能作为中间状态出现：
private void cancelAcquire(Node node) {
    node.waitStatus = Node.CANCELLED; // 标记为已取消
    // 清理节点逻辑...
}
注意：虽然 CANCELLED 是终态,但在清理过程中可能短暂出现 0.
```


3. waitStatus = 0 与其他状态的对比
```text
状态值	    常量名	            含义
0	        (无)	            初始状态或过渡状态
-1	        SIGNAL	            当前节点释放后需唤醒后继节点
-2	        CONDITION	        节点在条件队列中(如 Condition.await())
-3	        PROPAGATE	        共享模式下需传播唤醒信号
1	        CANCELLED	        线程已放弃等待(超时/中断)
```

4. 为什么需要 waitStatus = 0？
```text
状态初始化：所有新节点需要一个明确的初始值.

CAS 操作安全：
    在修改状态时(如 SIGNAL -> 0),需要明确的中间值保证原子性.
    避免直接从一个复杂状态切换到另一个状态(如 SIGNAL -> PROPAGATE)的竞态条件.

性能优化：
    0 是一个“无操作”状态,减少不必要的唤醒检查(如非 SIGNAL 节点无需处理后继节点).
```

5. 实际案例分析
```text
案例：ReentrantLock 的非公平锁
线程 A 获取锁成功,head 节点为 null.
线程 B 竞争锁失败,加入队列,其 Node.waitStatus 初始化为 0.
线程 C 竞争锁失败,加入队列,线程 B 的 waitStatus 被前驱节点(线程 C)改为 SIGNAL(因为线程 B 需要唤醒线程 C).
线程 A 释放锁时,发现 head.waitStatus = SIGNAL,于是唤醒线程 B,并将状态改回 0.

关键点：0 是 SIGNAL 和 PROPAGATE 状态转换的“桥梁”.
```

6. 总结
```text
waitStatus = 0 表示节点的 初始状态 或 临时过渡状态.

主要作用：
    作为新节点的默认值.
    在状态转换(如 SIGNAL -> 0 -> PROPAGATE)中保证 CAS 操作的安全性.
    避免不必要的同步开销.
与其他状态的关系：
    0 是 SIGNAL、PROPAGATE 等状态转换的中间态,体现了 AQS 对并发场景的精细控制.
```