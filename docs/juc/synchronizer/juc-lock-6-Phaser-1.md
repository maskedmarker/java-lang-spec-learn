# juc-Phaser

```text
中断

register/arriveAndAwaitAdvance/arrive 无视中断
internalAwaitAdvance有3种模式 无视中断/支持中断/支持超时
```


### doRegister

Implementation of register, bulkRegister.
Params:
        registrations – number to add to both parties and unarrived fields. Must be greater than zero.
Return: 返回参与者注册成功的phase💯💯💯

粗略地讲,
       如果当前phase处于进行中(参与者还未全部到达),state的parties部分和unarrived部分都加registrations
       如果当前phase已结束(参与者全部到达),就等待下个phase开始,然后state的parties部分和unarrived部分都加registrations
       因为是树形层级结构,还要考虑根节点和非根节点的情形

```text
private int doRegister(int registrations) {
    // adjustment to state
    long adjust = ((long)registrations << PARTIES_SHIFT) | registrations;
    final Phaser parent = this.parent;
    int phase;
    
    for (;;) {
        long s = (parent == null) ? state : reconcileState();                // s中的phase保持与根节点的phase一致,  每个循环先获取关键字段快照
        int counts = (int)s;
        int parties = counts >>> PARTIES_SHIFT;
        int unarrived = counts & UNARRIVED_MASK;
        if (registrations > MAX_PARTIES - parties)
            throw new IllegalStateException(badRegister(s));
        phase = (int)(s >>> PHASE_SHIFT);
        
        if (phase < 0)
            break;                                                             // 根节点的phase<0即terminated-bit已经被set,注册失败并返回负数phase,告知调用方Phaser已经terminated
        
        if (counts != EMPTY) {                                      // 当前节点已经有参与者注册
            if (parent == null || reconcileState() == s) {                     // 如果当前节点的phase与根节点的phase一致
                if (unarrived == 0)
                    root.internalAwaitAdvance(phase, null);                               // 如果phaser实例的参与者都已经到达了,当前参与者已经错过了这个phase,当前参与者需要等到下个phase开始才能恢复调度 (只有根节点支持internalAwaitAdvance方法)
                else if (UNSAFE.compareAndSwapLong(this, stateOffset, s, s + adjust))     
                    break;                                                                // 如果phaser实例的参与者还未都到达,当前参与者没有错过这个phase,则可增加state的parties部分和unarrived部分
            }
        }
        // 后面都是(counts==EMPTY)的情况,即phaser还没有参与者.
        
        else if (parent == null) {                                   // 如果是根节点第一个注册,增加state的parties部分和unarrived部分
            long next = ((long)phase << PHASE_SHIFT) | adjust;
            if (UNSAFE.compareAndSwapLong(this, stateOffset, s, next))
                break;
        }
        else {                                      // 如果是非根节点第一个注册
            synchronized (this) {               
                if (state == s) {                                     // 再次确认phase的state没有改变
                    phase = parent.doRegister(1);                     // 先在父节点上注册
                    
                    if (phase < 0)
                        break;                                        // 根节点的phase<0即terminated-bit已经被set,注册失败并返回负数phase
                    
                    // finish registration whenever parent registration succeeded, even when racing with termination, since these are part of the same "transaction".
                    // 通过while的重试,完成cas
                    while (!UNSAFE.compareAndSwapLong(this, stateOffset, s, ((long)phase << PHASE_SHIFT) | adjust)) {
                        s = state;                                  
                        phase = (int)(root.state >>> PHASE_SHIFT);   // cas失败重新读取state和root_phase
                        // assert (int)s == EMPTY;
                    }
                    break;
                }
            }
        }
    }
    return phase;
}
```


### doArrive

非最后一个到达者只需要cas操作parties和unarrived部分减少即可;
只有最后一个到达者: cas操作parties和unarrived部分减少/触发onAdvance/推进phase/唤醒等待线程/向parent传播普通arrival或deregister(特殊的arrival)

doArrive是Phaser的“原子心跳”: 它用一个CAS同时承载了到达计数/阶段推进/层级传播/终止控制.

(phase支持zero-wrapping循环且保证不会溢出符号位)phase单向自增💯
与普通phase推进相比,phaser终结只是多了设置terminated-bit部分.💯

本方法返回: 参与者到达的那个phase
本方法仅向phaser通报参与者到达,并会不等待其他参与者.
(arriveAndAwaitAdvance支持等待其他参与者,是为了进入下个phase,本方法并没有想要进入下一个phase的意图)

```text
private int doArrive(int adjust) {          // adjust是parties和unarrived部分的变动量的结合体,比如ONE_DEREGISTER/ONE_ARRIVAL
    final Phaser root = this.root;
    
    for (;;) {
        long s = (root == this) ? state : reconcileState();
        int phase = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;
        
        int counts = (int)s;
        int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);        // 判断没有注册需要使用(int)state == EMPTY这个方式
        if (unarrived <= 0)                                                       // 防御性校验非法arrive,防止arrive次数超过注册次数(不考虑放弃,理想情况下一个注册对应一个arrive)
            throw new IllegalStateException(badArrive(s));
        
        // 并发验证的线性化点
        if (UNSAFE.compareAndSwapLong(this, stateOffset, s, s-=adjust)) {          // cas更新state的parties和unarrived部分,如果cas失败,通过for循环重试
            // 如果原unarrived等于1,那么本次arrive将使原unarrived变为0,即全部参与者到达
            if (unarrived == 1) {
                // 变量n将作为本节点state的新值
                long n = s & PARTIES_MASK;                                          // 将原state的parties部分作为n的parties部分
                int nextUnarrived = (int)n >>> PARTIES_SHIFT;                       // 将原state的parties部分作为n的unarrived部分
                
                // 当前节点是根节点
                if (root == this) {
                    if (onAdvance(phase, nextUnarrived))                               // 💯💯💯只有根节点全部参与者到达才会有onAdvance; onAdvance返回true就表示phaser树整体终结terminated
                        n |= TERMINATION_BIT;                                          // 设置n的terminated-bit部分
                    
                    else if (nextUnarrived == 0)
                        n |= EMPTY;                                                    // 设置n的unarrived部分:特殊值
                    else
                        n |= nextUnarrived;                                            // 设置n的unarrived部分:非特殊值
                    
                    int nextPhase = (phase + 1) & MAX_PHASE;                          //  💯💯💯推进phase自增(支持zero-wrapping循环且保证不会溢出符号位)
                    n |= (long)nextPhase << PHASE_SHIFT;                              // 将原state的phase部分加一后作为n的phase部分
                    
                    UNSAFE.compareAndSwapLong(this, stateOffset, s, n);              // 为下一phase设置新的state值
                    
                    releaseWaiters(phase);                                           // 只有根节点全部参与者都到达时,才会取唤醒等待的参与者线程(唤醒所有awaitAdvance/arriveAndAwaitAdvance).   这是Phaser的“屏障释放点”💯💯💯
                }  
                // 后面都是非根节点
                
                else if (nextUnarrived == 0) { // propagate deregistration           // 非根节点全部参与者到达,如果下一phase没有参与者(unarrived为0,parties也为0),表示该子节点不再参与下一代phase,该节点要从parent注销
                    phase = parent.doArrive(ONE_DEREGISTER); 
                    UNSAFE.compareAndSwapLong(this, stateOffset, s, s | EMPTY);         // 💯💯💯先将parent的parties和unarrived减一,再将当前节点的count设置为空(这里延迟处理EMPTY特殊值是线程安全的,因为有cas引发的并发线性化点)
                }
                else
                    phase = parent.doArrive(ONE_ARRIVAL);                           // 💯💯💯非根节点全部参与者到达,如果下一phase还有参与者,将parent的unarrived减一,parties不变
            }
            return phase;
        }
    }
}

如果doArrive和forceTermination并发执行,会出现UNSAFE.compareAndSwapLong(this, stateOffset, s, n)执行失败,这里是合理的,phaser因为终结是没有下一个phase的.
```


### arriveAndAwaitAdvance

Arrives at this phaser and awaits others. 
Equivalent in effect to awaitAdvance(arrive()).

```text
public int arriveAndAwaitAdvance() {
    // Specialization of doArrive+awaitAdvance eliminating some reads/paths
    final Phaser root = this.root;
    
    // 存在多个线程同时到达而cas操作state,如果cas操作使用需要通过循环来重试
    for (;;) {
        long s = (root == this) ? state : reconcileState();
        int phase = (int)(s >>> PHASE_SHIFT);
        if (phase < 0)
            return phase;   // 如果Phaser已经终止,直接返回当前phase
        
        int counts = (int)s;
        int unarrived = (counts == EMPTY) ? 0 : (counts & UNARRIVED_MASK);
        if (unarrived <= 0)
            throw new IllegalStateException(badArrive(s));  // 防御性校验非法arrive,防止arrive次数超过注册次数(不考虑放弃,理想情况下一个register对应一个arrive)
        
        if (UNSAFE.compareAndSwapLong(this, stateOffset, s, s -= ONE_ARRIVAL)) {      // unarrived减一
            if (unarrived > 1)
                return root.internalAwaitAdvance(phase, null);     // 原unarrived>=2,unarrived减一后,还有参与者未到达,需要等待所有参与者到达
            
            // 原unarrived=1,当前参与者是最后一个到达的
            if (root != this)
                return parent.arriveAndAwaitAdvance();              // 非根节点的本节点都已全部到达,需要向父节点通知一个父节点层面的arrive
            
            
            // 下面的代码在(当前节点是根节点,且当前参与者是最后一个到达)条件下执行
            
            // 为下一个phase设置新的state值
            long n = s & PARTIES_MASK;                                   // 默认假定下一个phase的parties与本phase相同
            int nextUnarrived = (int)n >>> PARTIES_SHIFT;
            if (onAdvance(phase, nextUnarrived))
                n |= TERMINATION_BIT;
            else if (nextUnarrived == 0)
                n |= EMPTY;
            else
                n |= nextUnarrived;
            int nextPhase = (phase + 1) & MAX_PHASE;                     // phase加一
            n |= (long)nextPhase << PHASE_SHIFT;
            if (!UNSAFE.compareAndSwapLong(this, stateOffset, s, n))     // cas设置下一phase的state值
                // 当前参与者在根节点,且是最后一个到达的,此时cas竟然执行失败,只能是其他线程强制将phaser终止了(即调用forceTermination方法, forceTermination方法会唤醒所有等待者,所以当前方法不用再去执行唤醒操作)
                return (int)(state >>> PHASE_SHIFT);                      // 因为Phaser已经终结了,不存在下一个phase.这里返回的phase使用的是终结前的phase,非常准确
            
            releaseWaiters(phase);                                       // 最后一个到达者需要唤醒正在等待的(等待本循环开始时的phase结束)
            
            return nextPhase;
        }
    }
}
```


### awaitAdvance


Awaits the phase of this phaser to **advance from the given phase value**, returning immediately if the current phase is not equal to the given phase value or this phaser is terminated.

Params:
        phase – an arrival phase number; this argument is normally the value returned by a previous call to arrive or arriveAndDeregister.
Returns:
        the next arrival phase number, or the argument if it is negative, or the (negative) current phase if terminated

💯💯💯awaitAdvance方法,当前线程不仅有参与者调用,还有非参与者调用.

```text
public int awaitAdvance(int phase) {    // phase来自于returned by a previous call to arrive/arriveAndDeregister. 并随意的一个值会导致不确定行为
    final Phaser root = this.root;
    long s = (root == this) ? state : reconcileState();
    int p = (int)(s >>> PHASE_SHIFT);
    
    if (phase < 0)
        return phase;     // phaser已经terminated,phase不会再advance推进了,方法直接返回
    
    if (p == phase)
        return root.internalAwaitAdvance(phase, null);  // 当前phase还处于进行中,需要等待该phase结束
    
    // 如果p!=phase,意味着入参phase已经结束了,phase已经advance推进了
    return p;
}
```


### forceTermination

支持强制终止Phaser

```text
public void forceTermination() {
    // Only need to change root state
    final Phaser root = this.root;
    long s;
    
    while ((s = root.state) >= 0) {
        if (UNSAFE.compareAndSwapLong(root, stateOffset, s, s | TERMINATION_BIT)) {
            // 奇偶等待队列都唤醒
            releaseWaiters(0); // Waiters on evenQ
            releaseWaiters(1); // Waiters on oddQ
            return;
        }
    }
}
```




