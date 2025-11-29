# 关于并发编程中的术语


## 挂起（suspend)和阻塞（block)

```text
在 Java 语言和并发编程中，「挂起（suspend)」和「阻塞（block)」这两个词经常被混用，但它们在语义、作用层面和使用场景上是不同的。下面我来帮你系统地理清两者的区别，以及各自应该在什么情况下使用。

挂起（Suspend): 
        定义:     指线程主动让出 CPU 执行权，进入一种暂停运行但仍在内存中的状态。
      触发者:     通常是线程自己或框架/调度器决定暂时挂起(如 LockSupport.park)
     恢复方式:     需要显式调用
    典型 API:     LockSupport.park/unpark
    
阻塞（Block): 
        定义:    指线程在等待某个条件或资源（如 I/O、锁、信号量)时无法继续执行的状态。
       触发者:   通常是外部条件导致（如调用 Object.wait、Thread.sleep、或等待 I/O 完成)
     恢复方式:    当条件满足（I/O 完成、锁可用、notify 调用)时自动恢复执行
    典型 API:    wait/notify/sleep/monitorenter指令 (Thread.join不算,因为其底层通过wait实现的; 各种Lock的lock/await实现则是通过par/unpark实现的)
```

✅挂起（Suspend)和阻塞（Block)的描述层次就不同
* 挂起（Suspend)是在关于线程的CPU执行权层面,更微观点.
* 阻塞（Block)是在线程的执行方法的层面,更宏观点.

✅实际建议
* 如果你是写业务逻辑：用阻塞操作（wait、join、sleep、I/O 等)。
* 如果你是写并发框架、线程控制器、锁实现：用挂起机制（LockSupport.park/unpark)。


✅总结
* 挂起（Suspend)和阻塞（Block)的描述层次就不同
* 整个jvm层面,能导致线程挂起的底层只有park/unpark;能导致线程阻塞的底层只有wait/notify/sleep/monitorenter指令.
* 至于自旋spin的忙等待不是挂起,是阻塞
* 在锁(包括synchronized锁)的层面描述问题时,要使用阻塞(避免使用挂起),除非是解释juc.Lock的具体实现时才能用挂起.



* 导致线程发生阻塞而被挂起仅有可能发生在获取synchronized的monitor失败或者调用Object.wait或者调用park方法这3个方法.(至于自旋spin的忙等待不认为是阻塞)
* 而这3个方法都是锁相关,且均为锁的底层实现.
* 只有在线程被阻塞(或挂起)这段时间内,如果线程因为被调用了interrupt()方法而导致线程恢复调度并顺利执行完了方法体,此时我们就可以认为该锁方法支持中断.


AQS 的解决策略非常巧妙：它不强制要求“取消”操作必须原子性地完成所有指针的更新。相反，它选择了一个更可靠、更易维护的指针作为基准——即 prev 指针。

## lock-free double-linked FIFO queue

❗AQS的同步队列是一个FIFO的队列(它使用了lock-free double-linked FIFO queue)

![](E:\git-repo\cjh-repo\java-lang-spec-learn\docs\images\juc-aqs-lock-free-double-lined-FIFO-queue.png)
注意这里的prev和next与aqs相反

```text
一个无锁双向队列入队的正确顺序是：
写next → CAS tail → 写前驱的prev

这样可以保证链表结构在多线程环境下保持一致，而不需要 CAS 来更新 prev。

🧠为什么更新prev不需要CAS？
💡CAS tail成功的线程必然知道「它前面是谁(旧tail)」
💡只有成功CAS tail的线程会写该节点(旧tail)的prev
这就是 无锁但一致性强 的关键设计。



为什么需要“两阶段 prev 更新”？
第一阶段：快速写入 prev（但可能不稳定）
    ① 插入非常快，多线程并发入队时，prev 可能暂时不完全正确
    ② 有些线程可能被挂起、抢占，导致 prev 有“洞”
第二阶段：补齐所有 prev 缺口

🧠为什么 next 和 tail 要保证一致，而 prev 不必须立即一致？
    FIFO队列主结构使用next单向链表维持排队顺序(next-only 链就足以保证队列 FIFO)
    prev 主要用于高效执行取消操作（cancel）和唤醒（unpark）
        所以 prev 是“优化结构”，不是“主结构”。next 链必须 100% 立即正确,prev可以“稍后修复补全”
```