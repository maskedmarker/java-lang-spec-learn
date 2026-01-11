# juc-Phaser


## 源码实现


### reconcileState


💯💯💯phase指的是整体的phase,而非某个节点的phase值.
💯💯💯因为单根树形结构,根节点的phase值就是整体的phase值.

因为Phaser支持树形层级,而所有节点使用的都是同一个模型(Phaser类).
为了保持模型一致性,非根节点的state的phase部分不得不存在;其实根节点的state的phase部分就可以表示整体的phase.
为了管理方便,就允许非根节点的state的phase部分不要求准确性.
每次获取非根节点的state值时,通过拼接来达到非根节点的state值中的phase部分准确.

获取非根节点的state值时,就通过方法reconcileState来实现拼接.

```text
private long reconcileState() {
    final Phaser root = this.root;
    long s = state;
    
    if (root != this) {
        int phase, p;
        // CAS to root phase with current parties, tripping unarrived
        while ((phase = (int)(root.state >>> PHASE_SHIFT)) != (int)(s >>> PHASE_SHIFT) &&   // 如果根节点的phase与当前节点的phase不同,意味着根节点的phase已经结束了,子节点落后根节点
               !UNSAFE.compareAndSwapLong(this, stateOffset, s,                             // cas更新当前节点的state,如果cas失败则通过while循环重试
                s = (((long)phase << PHASE_SHIFT) |                                         // 当前节点的phase新值保持与根节点的phase一致
                     ((phase < 0) ? (s & COUNTS_MASK) :                                     // 如果根节点的phase<0(即根节点已经terminated),   -> 当前节点的state新值由(根节点的phase|当前节点的原parties和unarrived)构造
                      (((p = (int)s >>> PARTIES_SHIFT) == 0) ? EMPTY :                      // 如果根节点的phase>=0(即根节点未terminated),且当前节点的state的parties为0,按特殊值EMPTY处理   ->  当前节点的state新值由(根节点的phase|特殊值EMPTY)构造
                       ((s & PARTIES_MASK) | p))))))                                        // 如果根节点的phase>=0(即根节点未terminated),且当前节点的state的parties非0,                  ->  当前节点的state新值由(根节点的phase|当前节点的原parties|当前节点的原parties)构造    [unarrived==parties] : 因为本phase已经结束,unarrived恢复初始值
            s = state;
    }
    // else 如果this==root,取this.state就是root.state
    
    return s;
}
```

### internalAwaitAdvance

Possibly blocks and waits for phase to advance unless aborted. Call only on root phaser.
Params:
    phase – current phase (调用方认为自己等待的 phase)
    node – if non-null, the wait node to track interrupt and timeout; if null, denotes noninterruptible wait
Returns: 大于入参基准phase的最新phase

只能调用根节点的internalAwaitAdvance
phase单向自增(暂且忽略zero-wrapping)
在给定phase上等待推进,先自旋,再入队,再阻塞”,并正确处理中断/取消/phase变化.

💯💯💯等待栈中的线程都是调用了internalAwaitAdvance才完成了入栈.
💯💯💯当前线程不仅有参与者调用,还有非参与者调用.
💯💯💯参与者此时无需为phaser改动unarrived(要么已经减少unarrived了,要么要参与的阶段还未到来);非参与者更无需要了.

```text
private int internalAwaitAdvance(int phase, QNode node) {  // 可以通过入参node来设置支持中断/超时; 入参node为null就是不支持中断和超时
    // assert root == this;
    releaseWaiters(phase-1);          // 现在是phase阶段了,防止(phase-1)有未被唤醒的线程,他们等待的阶段已经结束了.为(phase+1)阶段准备.
    
    boolean queued = false;           // true when node is enqueued
    int lastUnarrived = 0;            // to increase spins upon change
    int spins = SPINS_PER_ARRIVAL;    // 自旋预算
    long s;
    int p;
    
    // 这是整个方法的正常情况下的“等待条件”: 只要phase没变,就继续等;异常情况指的是Phaser终止/等待被取消(中断/超时)
    while ((p = (int)((s = state) >>> PHASE_SHIFT)) == phase) {
        // 如果入参node为null,可以自旋等待一会
        if (node == null) {
            int unarrived = (int)s & UNARRIVED_MASK;
            
            // 如果unarrived发生变化且unarrived<CPU核数,说明推进可能很快完成,增加自旋预算,避免过早阻塞
            if (unarrived != lastUnarrived && (lastUnarrived = unarrived) < NCPU)
                spins += SPINS_PER_ARRIVAL;
            
            boolean interrupted = Thread.interrupted();
            // 自旋耗尽或者发生中断,需要创建QNode,进入可阻塞模式
            if (interrupted || --spins < 0) { 
                node = new QNode(this, phase, false, false, 0L);    // 该节点不支持中断和超时, QNode.phase为当前phase💯💯💯
                node.wasInterrupted = interrupted;
            }
        }
        
        else if (node.isReleasable()) // phase已推进/Phaser终止/等待被取消(中断/超时)
            break;
        
        else if (!queued) {           // 压入栈
            AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
            QNode q = node.next = head.get();
            if ((q == null || q.phase == phase) && (int)(state >>> PHASE_SHIFT) == phase) // 💯💯💯防止过期入队:phase已推进,但当前线程仍试图把自己挂到旧队列
                queued = head.compareAndSet(q, node);  // 存在cas入队失败重试和过期入队失败重试 (如果phase过期了,虽然无法入队,但在while-expression判断就为false,结束等待)
        }
        else {
            try {
                ForkJoinPool.managedBlock(node);              // 💯💯💯入队后,再进入阻塞
            } catch (InterruptedException ie) {
                node.wasInterrupted = true;
            }
        }
    }
    // 💯跳出while循环的条件:①phase已推进②当前线程被中断③等待超时 (phaser终结也属于phase推进)

    // 跳出主循环后的清理逻辑 💯💯💯   跳出主循环的原因有:phase已推进/Phaser终止/等待被取消(中断/超时)     | Phaser终止是phase已推进的特殊情形
    if (node != null) {
        if (node.thread != null)
            node.thread = null;                                          // 设置.thread=null也可以作为结束等待的标识(并非主要为了节省一次unpark这么简单)
        if (node.wasInterrupted && !node.interruptible)
            Thread.currentThread().interrupt();                          // 恢复中断语义
        
        if (p == phase && (p = (int)(state >>> PHASE_SHIFT)) == phase)   // phase没有推进就跳出了while循环,应该发生了非正常情况:当前线程被中断或等待超时或phaser被强制终结
            // 💯💯💯当前线程(参与者或非参与者)无修改unarrived的需要,不影响阶段的推进.纯粹就是为了等待下一个阶段的到来,此时被中断就不再等下一个阶段了.
            // 清除(X-2)阶段的等待线程.从internalAwaitAdvance开始阶段的releaseWaiters(phase-1)会将
            return abortWait(phase);
    }
    
    releaseWaiters(phase);  // 阶段结束了,唤醒正在等待下一个阶段到来的线程.
    
    return p;
}
```


## 无锁Treiber栈

等待栈是用来存储线程的引用,这些线程挂起后需要其他线程来唤醒,只有先持有线程引用才能后唤醒.唤醒后就没有必要再持有线程引用了,所以将引用从等待栈移除.
等待栈中的线程都是调用了internalAwaitAdvance才完成了入栈.💯💯💯

“合法的等待节点”指的是线程等待的阶段还未到来(即Node.phase==root.phase),且等待线程仍然愿意等待(未因中断/超时放弃).
“非法等待节点”具体包括的对应情形: phase已经推进但节点仍挂在旧队列中;等待线程已放弃但节点仍在队列中.

等待线程的引用是通过无锁Treiber栈保存的.
受限于栈操作(只有push/pop),没有遍历,只能操作栈顶.所以releaseWaiters/abortWait都只能操作栈顶的非法等待节点,一旦发现队头是合法的就立即停止.
“非法等待节点”并不会被立即清理,而是先做个标记(设置.thread=null),后续有空再清理,而不是想AQS那样,节点中断了就立即unlink掉.
Phaser在清理非法节点使用的是最终一致性,而AQS则是严格一致性.


参与者可以分为: arriveAndAwaitAdvance类 和 只有arrive没有awaitAdvance类的;
以及非参与者线程 : 没有arrive只有awaitAdvance类

phaser的使用可以分为:
    一阶段: 
        ①参与者发生中断超时,phase无法完成,用户代码来兜底负责,forceTermination唤醒挂起的参与者.
        ②非参与者awaitAdvance时就应该想清楚如何处理phase无法完成的情形,可以使用awaitAdvanceInterruptibly(timeout)防止睡死,同时forceTermination唤醒挂起的参与者.如果用户不做兜底,可能会发生睡死.
    多阶段
        类似于一阶段的①/②情形
所以phaser的用户代码会兜底异常情形,才能保证程序正常不出现睡死.同时forceTermination也会清空等待栈中对象.

abortWait/releaseWaiters方法本身在清理“非法等待节点”时,由于只能操作栈顶元素而非遍历,所以无法做到实时全部清理“非法等待节点”.但是,
如果参与者都没有发生中断/超时,最终所有等待节点(包括“非法等待节点”,即也包括非参与者的“非法等待节点”)都会被清理;
如果参与者有发生中断/超时,通过兜底强制终结phaser,最终所有等待节点(包括“非法等待节点”,即也包括非参与者的“非法等待节点”)都会被清理;

因中断/超时放弃等待的线程已经恢复调度且跳出了循环,不再需要唤醒.

### abortWait

只有internalAwaitAdvance在使用abortWait,请勿随意扩展上下文.💯💯💯
abortWait只作用在当前正在进行中的phase(从调用方源代码提取的信息)

仅仅是恢复了线程执行能力,不会改变外层的循环(即internalAwaitAdvance的while循环)逻辑.


```text
private int abortWait(int phase) {     // 入参phase表示的正在进行中的phase(从调用方源代码提取的信息)
    AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
    
    for (;;) {
        Thread t;
        QNode q = head.get();
        
        int p = (int)(root.state >>> PHASE_SHIFT);
        if (q == null || ((t = q.thread) != null && q.phase == p))  // abortWait与releaseWaiters循环退出的条件相比,多了(q.thread != null) 💯💯有限尽力清除
            return p;
        
        if (head.compareAndSet(q, q.next) && t != null) {
            q.thread = null;
            LockSupport.unpark(t);
        }
    }
}
```

### releaseWaiters

releaseWaiters都是在等待阶段结束后调用的(从调用方源代码提取的信息).

唤醒等待特定phase结束的参与者.
    如果当前阶段与等待的阶段不同,就代表等待的阶段已经结束

releaseWaiters与abortWait循环退出的条件相比,少了(q.thread != null),这样internalAwaitAdvance的开始处有releaseWaiters(phase-1)的操作可以清除所有的节点(包括非法节点).💯💯💯

```text
private void releaseWaiters(int phase) {    // 入参phase表示是已结束的阶段
    QNode q;   // first element of queue
    Thread t;  // its thread
    AtomicReference<QNode> head = (phase & 1) == 0 ? evenQ : oddQ;
    
    // 等待栈中的线程都是调用了internalAwaitAdvance才完成了入栈,在internalAwaitAdvance的开始处有releaseWaiters(phase-1)的操作.所以保证(X-2)与X阶段不会同时出现在同一个等待栈中.💯💯💯
    while ((q = head.get()) != null && q.phase != (int)(root.state >>> PHASE_SHIFT)) { // 💯q.phase != (int)(root.state >>> PHASE_SHIFT)表示线程等待的阶段已经结束
        if (head.compareAndSet(q, q.next) && (t = q.thread) != null) {
            q.thread = null;
            LockSupport.unpark(t);   // 唤醒等待的线程
        }
    }
}

等待阶段结束后唤醒等待线程,这些线程会自己通过isReleasable()判断可以结束internalAwaitAdvance的等待.
```