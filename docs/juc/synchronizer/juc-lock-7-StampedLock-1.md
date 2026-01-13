# juc-StampedLock


## 源码实现

### 构造函数

```text

```

### 常量
```text
// 每加一个读锁,就在reader-count部分加一
RUNIT = 1L;

// state中,reader-count能使用的bit数  (支持最大127的读并行度)
LG_READERS = 7;

// 是否有写锁的标识bit位
WBIT  = 1L << LG_READERS;

// reader-count的mask
RBITS = WBIT - 1L;
// reader-count最大值减一作为临界判断, state中reader-count最大可以是WBIT-1L,再然后就启用readerOverflow
RFULL = RBITS - 1L;

// 如果(state & ABITS) == 0,意味着reader-count为0,写锁的标识bit位也没有设置,即表示当前没有加锁. 
ABITS = RBITS | WBIT;

// 从state中获取stamp (即state中剔除reader-count部分的数据作为stamp)
SBITS = ~RBITS;
```

### 关键字段

```text
// 
private transient volatile long state;



// 当state中reader-counter计数器不够用时,再启用该字段(极少场景)
private transient int readerOverflow;
```

## 写锁

### writeLock



```text
public long writeLock() {
    long s, next;  // bypass acquireWrite in fully unlocked case only
    return ((((s = state) & ABITS) == 0L &&
             U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
            next : acquireWrite(false, 0L));
}

等价转换
public long writeLock() {
    long s, next;  // bypass acquireWrite in fully unlocked case only
    
    if(((s = state) & ABITS) == 0L){                                          // 如果未加锁
        if(U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) {
            return next;                                                      // 加写锁成功,返回此时的state值
        }
    }
    
    // 当前是要加排他写锁,如果已经加锁(不管是读写),都要等待锁释放后去抢占
    return acquireWrite(false, 0L);
}
```


## 读锁

### tryOptimisticRead


```text
public long tryOptimisticRead() {
    long s;
    return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
}

等价转换
public long tryOptimisticRead() {
    long s;
    
    if(((s = state) & WBIT) == 0L){     // 如果没有加写锁
        return (s & SBITS);             // 返回state中剔除reader-count部分的数据作为stamp
    }
    
    // 如果已经加写锁返回0,表示无法获取stamp
    return 0L;
}
```

### validate

true if the lock has not been exclusively acquired since issuance of the given stamp; else false.

```text
public boolean validate(long stamp) {
    // 刷新缓存,为了获取最新的state值
    U.loadFence();
    
    return (stamp & SBITS) == (state & SBITS);   // state中stamp没有改变(忽视reader-count,因为已存在的读锁不影响当前要获取的读锁)
}
```

### readLock

RFULL = RBITS - 1L;

```text
public long readLock() {
    long s = state, next;  // bypass acquireRead on common uncontended case
    return ((whead == wtail && (s & ABITS) < RFULL &&
             U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
            next : acquireRead(false, 0L));
}

等价转换
public long readLock() {
    long s = state, next;  // bypass acquireRead on common uncontended case
    
    // 写锁的等待队列为空,且读锁容量还未满(支持的读并行度是有限的)
    if(whead == wtail && (s & ABITS) < RFULL) {
        if(U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) {    // 每加一个读锁,就在reader-count部分加一 ((s & ABITS) < RFULL的判断可以防止reader-count发生溢出)
            return next;
        }
    }
    
    return acquireRead(false, 0L);
}

```

## 核心方法

### acquireWrite

自旋尝试获取
           ->入队
                 ->如果条件满足再自旋尝试获取
                 ->协助唤醒头节点的cowait队列线程(头节点一定是读锁节点)
                 ->在正常的前节点上设置WAITING标识
                 ->挂起当前线程(等待条件合适的时候被唤醒)


```text
private long acquireWrite(boolean interruptible, long deadline) {
    // node指向入队的新节点, p指向新节点的前节点
    WNode node = null, p;
    
    // 💯💯第一阶段: 等待队列为空,很可能即将释放锁,允许短时间自旋
    for (int spins = -1;;) {
        long m, s, ns;
        if ((m = (s = state) & ABITS) == 0L) {                           // 如果当前没有加锁,则试图用CAS完成加写锁
            if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))
                return ns;                                               // 自旋过程中加写锁成功,不用等待,提前结束
        }
        
        
        else if (spins < 0)
            spins = (m == WBIT && wtail == whead) ? SPINS : 0;           // 如果等待队列为空,且没有读锁,只有写锁,触发自旋初始化
        else if (spins > 0) {                                            // 自旋退让
            if (LockSupport.nextSecondarySeed() >= 0)                         // 随机退避,减少总线竞争
                --spins;
        }
        
        // 后面都是自旋结束后发生的.也使用了CLH队列技术
        
        else if ((p = wtail) == null) {                                  // CLH队列的初始化
            WNode hd = new WNode(WMODE, null);  // dummy-node是写模式
            if (U.compareAndSwapObject(this, WHEAD, null, hd))
                wtail = hd;
        }
        else if (node == null)
            node = new WNode(WMODE, p);                                  // 创建CLH队列的节点对象(节点是写模式)
        else if (node.prev != p)
            node.prev = p;
        
        else if (U.compareAndSwapObject(this, WTAIL, p, node)) {         // CLH队列尾部插入新节点
            p.next = node;
            break;                                                       // 入队成功,结束循环进入下一阶段
        }
    }

    // 💯💯第二阶段: 队首竞争自旋+阻塞等待
    for (int spins = -1;;) {
        WNode h, np, pp; int ps;
        
        // 如果当前线程入队后,发现自己是等待队列的第一线程节点,允许自旋来竞争写锁
        if ((h = whead) == p) { // 如果是第一线程节点
            
            if (spins < 0)                     // 第一次当第一线程节点时的自旋初始化
                spins = HEAD_SPINS;
            else if (spins < MAX_HEAD_SPINS)   // 非第一次当第一线程节点时的自旋初始化
                spins <<= 1;                        // 自旋的机会减半
            
            for (int k = spins;;) { // 比较激进,可以多次尝试竞争写锁,而非一次
                long s, ns;
                if (((s = state) & ABITS) == 0L) {                                 // 无锁时才能CAS去抢占写锁
                    if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT)) {
                        whead = node;                                              // 抢占写锁成功后,更新头节点
                        node.prev = null;
                        return ns;
                    }
                }
                else if (LockSupport.nextSecondarySeed() >= 0 && --k <= 0)  // 自旋耗尽,结束自旋
                    break;
            }
        }
        else if (h != null) {         // 如果不是第一线程节点,激活头节点的cowait线程们,让他们恢复调度然后自旋抢占锁
            WNode c; Thread w;
            while ((c = h.cowait) != null) {             // 将cowait队列的元素从队头一个个移除并唤醒
                if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                    (w = c.thread) != null)
                    U.unpark(w);
            }
        }
        
        
        // 当头节点变化稳定后:退避热竞争
        if (whead == h) {
            
            // 如果前节点已经取消等待了,跳过取消节点,然后在有效节点上设置WAITING标识(注意:这些操作是多个循环重试完成的💯💯💯)
            if ((np = node.prev) != p) {
                if (np != null)
                    (p = np).next = node;   // stale
            }
            else if ((ps = p.status) == 0)
                U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
            else if (ps == CANCELLED) {
                if ((pp = p.prev) != null) {
                    node.prev = pp;
                    pp.next = node;
                }
            }
            
            // 已经设置好WAITING标识,准备进入挂起等待(注意:这些操作是多个循环重试完成的💯💯💯)
            
            else {
                // 处理超时
                long time; // 0 argument to park means no timeout
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)
                    return cancelWaiter(node, node, false);
                
                Thread wt = Thread.currentThread();
                U.putObject(wt, PARKBLOCKER, this);
                node.thread = wt;
                if (p.status < 0 && (p != h || (state & ABITS) != 0L) && whead == h && node.prev == p)    // 挂起的必要条件:(前驱节点已经设置WAITING ) && (当前线程非第一线程节点 或 其他线程已经持有锁) && (瞬间稳态: whead/node.prev没有发生变化)
                    U.park(false, time);
                node.thread = null;
                U.putObject(wt, PARKBLOCKER, null);
                
                if (interruptible && Thread.interrupted())
                    return cancelWaiter(node, node, true);
            }
        }
    }
}
```

### acquireRead

该方法是StampedLock最复杂的路径之一,因为它同时处理: 读者并发/写者优先级/读-写公平性/cowait(读合并)/自旋+阻塞/超时/中断

目标：在保证写者不会被无限饿死的前提下,尽可能让读者并发通过.
核心设计原则：
    无写等待时：读锁应接近无锁
    有写等待时：读者必须排队
    队首读者可以批量放行（cowait）
    写者一旦可见，读者逐步退让

StampedLock 的读锁语义
条件	          |   行为
--------------------------------
无写锁+无写等待  | 读者直接CAS
无写锁+有写等待  |	读者排队
队首是读        | 批量读者通过
队首是写        | 读者阻塞
--------------------------------

```text
private long acquireRead(boolean interruptible, long deadline) {
    WNode node = null, p;
    
    // 💯💯第一阶段: 等待队列为空,可能即将释放锁,允许短时间自旋
    //              读锁会在下一个写锁线程到来前(忽略reader-count容量不够的极端场景)保持自旋等待一会
    for (int spins = -1;;) {
        WNode h;
        if ((h = whead) == (p = wtail)) {
            for (long m, s, ns;;) {
                if ((m = (s = state) & ABITS) < RFULL ?                                                                          // 💯最理想的情况: 没有等待且没有写锁, 直接cas读锁数量加一就结束了
                    U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) : (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L))         // reader-count数量加一(state最大容量不够的话,在readerOverflow上累加,这是小概率极端场景)
                    return ns;  
                else if (m >= WBIT) {                                                                                            //  没有等待但有写锁,先自旋等待写锁释放                                                    
                    if (spins > 0) {                                                 // 自旋计数器随机减少(防止并发线程同频)
                        if (LockSupport.nextSecondarySeed() >= 0)
                            --spins;
                    }
                    else {                                                           
                        if (spins == 0) {                                            
                            WNode nh = whead, np = wtail;
                            if ((nh == h && np == p) || (h = nh) != (p = np))         // 注意自旋周期结束时,如果首尾节点有变化则更新变量h/p
                                // 自旋计数器减少到0时,如果整个自旋周期内CLH的头尾节点没有变化,或者等待队列已经为非空了(有写锁线程在等待,或者读锁线程自旋够了),则结束自旋,准备入队挂起等待
                                // 对应场景: ①写锁的线程没有在自旋周期内释放写锁,估计短时间内也不会释放写锁 ②写锁线程看到有线程持有锁,直接挂起等待,不像读锁线程先自旋一会
                                break;                                                
                        }
                        spins = SPINS;                                                // 初始化自旋计数器
                    }
                }
            }
        }
        
        // 注意: 入队的操作是在多次循环中完成的
        // 💯💯第二阶段: 
                        在主等待队列入队(还未挂起等待)
                        或者合并到主队列的读节点的cowait队列中(连续的读锁节点会合并到一个CLH节点中,即使用cowait队列),然后等待
        if (p == null) {                                         // CLH队列的初始化
            WNode hd = new WNode(WMODE, null); // dummy-node也是写模式
            if (U.compareAndSwapObject(this, WHEAD, null, hd))
                wtail = hd;
        }
        else if (node == null)
            node = new WNode(RMODE, p);                                        // 创建等待节点(读模式)
        else if (h == p || p.mode != RMODE) {                                  // 如果等待队列只有dummy-node或者尾节点是写锁线程的,才入队💯
            if (node.prev != p)
                node.prev = p;
            else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                p.next = node;
                break;                                                        // 💯如果入队CLH等待队列,进入下一阶段
            }
        }
        else if (!U.compareAndSwapObject(p, WCOWAIT, node.cowait = p.cowait, node))  // 如果尾节点是读锁线程的,直接尾节点的cowait队列队首入队,即连续读锁节点会合并💯💯💯
            node.cowait = null;   // 如果入队cowait的cas失败则重试
        else {                                                                       // 💯如果入队cowait队列
            for (;;) {
                WNode pp, c; Thread w;
                // CLH头节点对应的线程已经释放锁,协助唤醒cowait队列的线程(即读锁节点及其连续的读锁线程)
                if ((h = whead) != null && (c = h.cowait) != null && U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) && (w = c.thread) != null)
                    U.unpark(w);
                
                
                // 当前线程是入队cowait队列,对应的p即为当前线程所处的CLH队列的节点(即主等待队列) (CLH队列中,只有首节点的下个节点即第一线程节点才能去尝试抢占锁)
                // 如果位于主等待队列的第一线程节点(h==p.prev),或者此时已经是首节点(h==p或者p.prev==null 因为瞬时态会出现刚判断完h!=p后发生了被设置为首节点,此时p.prev==null),此时更容易获取读锁
                if (h == (pp = p.prev) || h == p || pp == null) {
                    long m, s, ns;
                    // 如果此时没有写锁,尝试获取读锁
                    do {
                        if ((m = (s = state) & ABITS) < RFULL ? U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) : (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L))
                            return ns;
                    } while (m < WBIT);
                }
                
                if (whead == h && p.prev == pp) {
                    long time;
                    if (pp == null || h == p || p.status > 0) {    // p.status > 0意味着前节点发生取消了
                        node = null;                               // 入队的cowait的主队列节点发生了取消(WAITING标识没了),不管当前线程在cowait中的节点,重新开始外外层的for循环
                        break;
                    }
                    
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, p, false);
                    
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if ((h != pp || (state & ABITS) == WBIT)      // 非第一线程节点或有写锁才能挂起
                                 && whead == h && p.prev == pp)   // 防御编程,保证队列未发生改变
                        U.park(false, time);
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    
                    if (interruptible && Thread.interrupted())
                        return cancelWaiter(node, p, true);
                }
            }
        }
    }

    // 💯💯第三阶段: 主队列节点,自旋尝试后,挂起等待
    for (int spins = -1;;) {
        WNode h, np, pp; int ps;
        // 如果等待队列为空时,自旋尝试获取读锁
        if ((h = whead) == p) {
            if (spins < 0)
                spins = HEAD_SPINS;
            else if (spins < MAX_HEAD_SPINS)
                spins <<= 1;
            for (int k = spins;;) { // spin at head
                long m, s, ns;
                if ((m = (s = state) & ABITS) < RFULL ? U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) : (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {   // 如果没有写锁,读锁加一
                    WNode c; Thread w;
                    whead = node;
                    node.prev = null;
                    while ((c = node.cowait) != null) {                                                             // 唤醒cowait队列平级的读锁线程
                        if (U.compareAndSwapObject(node, WCOWAIT, c, c.cowait) && (w = c.thread) != null)
                            U.unpark(w);
                    }
                    return ns;
                }
                else if (m >= WBIT && LockSupport.nextSecondarySeed() >= 0 && --k <= 0)    // 自旋中如果有写锁了就放弃自旋
                    break;
            }
        }
        else if (h != null) {                                  // 如果发现等待队列不为空,协助唤醒头节点的cowait队列的线程
            WNode c; Thread w;
            while ((c = h.cowait) != null) {
                if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) && (w = c.thread) != null)
                    U.unpark(w);
            }
        }
        
        if (whead == h) {
            if ((np = node.prev) != p) {
                if (np != null)
                    (p = np).next = node;   // stale
            }
            else if ((ps = p.status) == 0)
                U.compareAndSwapInt(p, WSTATUS, 0, WAITING);        // 在前节点设置WAITING
            else if (ps == CANCELLED) {
                if ((pp = p.prev) != null) {
                    node.prev = pp;
                    pp.next = node;                                // 移除取消等待的节点
                }
            }
            else {
                long time;
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)
                    return cancelWaiter(node, node, false);
                
                Thread wt = Thread.currentThread();
                U.putObject(wt, PARKBLOCKER, this);
                node.thread = wt;
                if (p.status < 0 && (p != h || (state & ABITS) == WBIT) &&      // 前节点已经设置WAITING 且 非第一线程节点或有写锁才能挂起
                    whead == h && node.prev == p)               // 防御编程,保证队列未发生改变
                    U.park(false, time);
                node.thread = null;
                U.putObject(wt, PARKBLOCKER, null);
                
                if (interruptible && Thread.interrupted())
                    return cancelWaiter(node, node, true);
            }
        }
    }
}
```

### tryIncReaderOverflow

```text
private long tryIncReaderOverflow(long s) {
    // assert (s & ABITS) >= RFULL;
    
    if ((s & ABITS) == RFULL) {
        if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {    // state中reader-count可以最大到RBITS,即WBIT-1L,然后在readerOverflow累加
            ++readerOverflow;
            state = s;
            return s;
        }
    }
    else if ((LockSupport.nextSecondarySeed() & OVERFLOW_YIELD_RATE) == 0)
        Thread.yield();
    return 0L;
}
```

### cancelWaiter


负责在中断/超时/取消 时：
修复cowait链/修复主写队列/必要时主动推进队首节点
返回正确的 stamp / 中断标记

```text
private long cancelWaiter(WNode node, WNode group, boolean interrupted) {    // 如果当前节点是主队列节点,则在调用cancelWaiter时,入参node==group
    // ???
    if (node != null && group != null) {
        Thread w;
        node.status = CANCELLED;
        
        // 移除groupd的cowait队列中已经取消的节点
        for (WNode p = group, q; (q = p.cowait) != null;) {
            if (q.status == CANCELLED) {
                U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                p = group;  // 防止并发修改的cas失败导致遗漏
            }
            else
                p = q;
        }
        
        // 当前线程是主队列节点
        if (group == node) {
            // 唤醒所有未取消的 cowait 线程
            for (WNode r = group.cowait; r != null; r = r.cowait) {
                if ((w = r.thread) != null)
                    U.unpark(w);
            }
            
            // 从主队列中“摘除”当前node(最复杂部分)💯💯💯
            for (WNode pred = node.prev; pred != null; ) {
                WNode succ, pp;        
                while ((succ = node.next) == null || succ.status == CANCELLED) {                // find valid successor
                    WNode q = null;
                    for (WNode t = wtail; t != null && t != node; t = t.prev)
                        if (t.status != CANCELLED)
                            q = t; 
                    
                    if (succ == q || U.compareAndSwapObject(node, WNEXT, succ, succ = q)) {     // 当前节点的next指向一个正常的后续节点
                        if (succ == null && node == wtail)
                            U.compareAndSwapObject(this, WTAIL, node, pred);                    // 如果当前节点是尾节点,需要更新尾节点指针
                        break;
                    }
                }
                
                if (pred.next == node) 
                    U.compareAndSwapObject(pred, WNEXT, node, succ);                             // 设置pred.next
                if (succ != null && (w = succ.thread) != null) {
                    succ.thread = null;
                    U.unpark(w);                                                                 // 当前节点正在取消等待,需要唤醒一个其后续节点,弥补可能发生在本节点上的并发唤醒被浪费
                }
                if (pred.status != CANCELLED || (pp = pred.prev) == null)
                    break;
                
                // 此时(pred.status==CANCELLED 且pred.prev!=null),即当前节点的前节点发生了取消且当前节点不是头节点,需要循环继续
                node.prev = pp;
                U.compareAndSwapObject(pp, WNEXT, pred, succ);   // 修复next链,即修复unpark唤醒链
                pred = pp;
            }
        }
    }
    
    
    WNode h; // Possibly release first waiter
    while ((h = whead) != null) {
        long s; WNode q;                                          // similar to release() but check eligibility
        if ((q = h.next) == null || q.status == CANCELLED) {
            for (WNode t = wtail; t != null && t != h; t = t.prev)
                if (t.status <= 0)
                    q = t;
        }
        
        if (h == whead) {
            if (q != null && h.status == 0 &&
                ((s = state) & ABITS) != WBIT && // waiter is eligible
                (s == 0L || q.mode == RMODE))
                release(h);
            break;
        }
    }
    
    return (interrupted || Thread.interrupted()) ? INTERRUPTED : 0L;
}
```


### release

Wakes up the successor of h (normally whead). This is normally just h.next, but may require traversal from wtail if next pointers are lagging. 
This may fail to wake up an acquiring thread when one or more have been cancelled, **but the cancel methods themselves provide extra safeguards to ensure liveness**.

```text
private void release(WNode h) {
    if (h != null) {
        WNode q; Thread w;
        U.compareAndSwapInt(h, WSTATUS, WAITING, 0);
        
        if ((q = h.next) == null || q.status == CANCELLED) {
            for (WNode t = wtail; t != null && t != h; t = t.prev)
                if (t.status <= 0)
                    q = t;
        }
        
        if (q != null && (w = q.thread) != null)
            U.unpark(w);
    }
}
```

### tryConvertToWriteLock

```text
public long tryConvertToWriteLock(long stamp) {
    long a = stamp & ABITS, m, s, next;
    
    while (((s = state) & SBITS) == (stamp & SBITS)) {   // 在stamp不变的前提下
        if ((m = s & ABITS) == 0L) {                                            // 如果此时没有锁
            if (a != 0L)                                                                 // 如果入参stamp中有锁,此时无锁,此时stamp不一致,则结束尝试
                break;
            if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))                   // 加写锁
                return next;
        }
        else if (m == WBIT) {                                                    // 如果此时有写锁
            if (a != m)                                                                        // 如果入参stamp中没有写锁,此时有写锁,stamp不一致,则结束尝试
                break;
            return stamp;
        }
        else if (m == RUNIT && a != 0L) {                                        // 如果此时只有一个读锁,入参stamp有写锁
            if (U.compareAndSwapLong(this, STATE, s, next = s - RUNIT + WBIT))        // cas撞大运,看看是否有读锁刚好释放
                return next;
        }
        else
            break;
    }
    
    // stamp不可用返回0表示失败
    return 0L;
}
```