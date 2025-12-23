# 关于并发编程中的术语


## 挂起（suspend)和阻塞（block)

```text
在 Java 语言和并发编程中,「挂起（suspend)」和「阻塞（block)」这两个词经常被混用,但它们在语义、作用层面和使用场景上是不同的.下面我来帮你系统地理清两者的区别,以及各自应该在什么情况下使用.

挂起（Suspend): 
        定义:     指线程主动让出 CPU 执行权,进入一种暂停运行但仍在内存中的状态.
      触发者:     通常是线程自己或框架/调度器决定暂时挂起(如 LockSupport.park)
     恢复方式:     需要显式调用
    典型 API:     LockSupport.park/unpark
    
阻塞（Block): 
        定义:    指线程在等待某个条件或资源（如 I/O、锁、信号量)时无法继续执行的状态.
       触发者:   通常是外部条件导致（如调用 Object.wait、Thread.sleep、或等待 I/O 完成)
     恢复方式:    当条件满足（I/O 完成、锁可用、notify 调用)时自动恢复执行
    典型 API:    wait/notify/sleep/monitorenter指令 (Thread.join不算,因为其底层通过wait实现的; 各种Lock的lock/await实现则是通过par/unpark实现的)
```

✅挂起（Suspend)和阻塞（Block)的描述层次就不同
* 挂起（Suspend)是在关于线程的CPU执行权层面,更微观点.
* 阻塞（Block)是在线程的执行方法的层面,更宏观点.

✅实际建议
* 如果你是写业务逻辑：用阻塞操作（wait、join、sleep、I/O 等).
* 如果你是写并发框架、线程控制器、锁实现：用挂起机制（LockSupport.park/unpark).


✅总结
* 挂起（Suspend)和阻塞（Block)的描述层次就不同
* 整个jvm层面,能导致线程挂起的底层只有park/unpark;能导致线程阻塞的底层只有wait/notify/sleep/monitorenter指令.
* 至于自旋spin的忙等待不是挂起,是阻塞
* 在锁(包括synchronized锁)的层面描述问题时,要使用阻塞(避免使用挂起),除非是解释juc.Lock的具体实现时才能用挂起.



* 导致线程发生阻塞而被挂起仅有可能发生在获取synchronized的monitor失败或者调用Object.wait或者调用park方法这3个方法.(至于自旋spin的忙等待不认为是阻塞)
* 而这3个方法都是锁相关,且均为锁的底层实现.
* 只有在线程被阻塞(或挂起)这段时间内,如果线程因为被调用了interrupt()方法而导致线程恢复调度并顺利执行完了方法体,此时我们就可以认为该锁方法支持中断.



## lock-free double-linked FIFO queue

❗AQS的同步队列是一个FIFO的队列(它使用了lock-free double-linked FIFO queue)

![](E:\git-repo\cjh-repo\java-lang-spec-learn\docs\images\juc-aqs-lock-free-double-lined-FIFO-queue.png)
注意这里的prev和next与aqs相反

```text
一个无锁双向队列入队的正确顺序是：
写next → CAS tail → 写前驱的prev

这样可以保证链表结构在多线程环境下保持一致,而不需要 CAS 来更新 prev.

🧠为什么更新prev不需要CAS？
💡CAS tail成功的线程必然知道「它前面是谁(旧tail)」
💡只有成功CAS tail的线程会写该节点(旧tail)的prev
这就是 无锁但一致性强 的关键设计.



为什么需要“两阶段 prev 更新”？
第一阶段：快速写入 prev（但可能不稳定）
    ① 插入非常快,多线程并发入队时,prev 可能暂时不完全正确
    ② 有些线程可能被挂起、抢占,导致 prev 有“洞”
第二阶段：补齐所有 prev 缺口

🧠为什么 next 和 tail 要保证一致,而 prev 不必须立即一致？
    FIFO队列主结构使用next单向链表维持排队顺序(next-only 链就足以保证队列 FIFO)
    prev 主要用于高效执行取消操作（cancel）和唤醒（unpark）
        所以 prev 是“优化结构”,不是“主结构”.next 链必须 100% 立即正确,prev可以“稍后修复补全”
```

## 并发编程细节
锁的特性: 
互斥性: 同一把锁在同一时刻最多只能被一个线程持有
不可抢占: 线程持有锁之后,除非该线程主动释放锁,否则将一直持有
可见性: 加锁和解锁操作会隐含地插入内存屏障


```text
double-check

// 通用的模板
public void doFoo1() {
   acquire(lock){   // 在抢到锁以前需要等待                                  
      while(biz-condition-is-not-satisfied){            
            release-lock-and-wait;                        // 等待条件满足,否者无限期等待(依赖其他线程在条件满足时唤醒当前线程)
      }
      execute-code-in-critical-section;
   }  // 这里释放锁
}

public void doFoo2() {
   acquire(lock){                               
      while(biz-condition-is-not-satisfied){            
            release-lock-and-waitWithTimeout;             // 等待条件满足,否者无限期等待(因为有超时机制,不依赖其他线程在条件满足时的唤醒,当前线程可以在超时后主动检测条件是否满足)
      }
      execute-code-in-critical-section;
   }
}

public void doFoo3() {
   if(biz-condition-is-satisfied) {                         // ①
       acquire(lock){                                       // ②
          if(biz-condition-is-not-satisfied){               // ①②之间是有执行空隙的,在这个空隙biz-condition会发生变化,所以需要二次检查biz-condition
                execute-code-in-critical-section;
          } else {
                do-other;                                   // 在有选择权的时候,可以不用无限期等待条件满足;加锁仅仅是为原子操作
          }
       }
   } else {
       do-other;                                            // 在有选择权的时候,可以不用无限期等待条件满足
   }
}


// monitor实现
public void doFoo1() {
   synchronized(lock){                    
      while(biz-condition-is-not-satisfied){            
            lock.wait();                        
      }
      execute-code-in-critical-section;
   } 
}

public void doFoo2() {
   synchronized(lock){                       
      while(biz-condition-is-not-satisfied){            
            lock.wait(timeout);
      }
      execute-code-in-critical-section;
   }
}

public void doFoo3() {
   if(biz-condition-is-satisfied) {
       synchronized(lock){
          if(biz-condition-is-not-satisfied){
                execute-code-in-critical-section;
          } else {
                do-other;                                   
          }
       }
   } else {
       do-other;                                            
   }
}


// cas的实现
如果想通过cas来实现doFoo1/doFoo2中的锁机制,最后实现的锁机制大致等价于不支持取消操作的AQS;

public void doFoo3() {
    if (lockStatus == unlocked && cas(lockStatus, locked)) {  //  防止违反了锁的不可抢占性: 先(lockStatus == unlocked)来检查锁没有被持有,然后才能通过cas(status_unlocked, locked)来占有锁
        execute-code-in-critical-section;
    } else {
        do-other;
    }
}
```