# juc-AQS-方法注释-AbstractQueuedSynchronizer


```text
挂起（Suspend)和阻塞（Block)的描述层次就不同:
    当站在锁的设计者角度,当调用park时,可以用挂起来表述;
    当站在锁的使用者角度,当因为方法内部使用了park方法导致当前线程无法继续执行时,用阻塞来表述.
```

```text
⚠️ AQS是无锁同步器,底层主要依靠:
1. 仅有CAS能保证test-and-set的原子性,任意2个语句都存在并发竞态问题.
2. CAS的原子性是保证算法正确的一个重要依靠支点;CAS/volatile的内存读写屏障也是保证算法正确的另一个重要的依靠支点.
```


```text
⚠️AQS基于CLH的lock-free queue-based Lock算法,在此基础之上新增了取消机制.
⚠️AQS真正要解决的核心是如何处理cancel和signal的并发问题.即在不遗漏signal的情况下完成cancel操作.
```



```text
AQS基于CLH的lock-free queue-based Lock算法.同时又使用优化写入方法实现一个cas完成节点入队.

CLH是基于一个FIFO的队列.
    CLH节点只有一个prev指针;
    CLH队列只有head/tail 2个指针.
CLH节点的waitStatus
    只能后节点设置和自己节点退出时设置.
    waitStatus值变化是单向的,
        0/PROPAGATE->signaled->cancelled
        0/PROPAGATE->cancelled


优化写入方法依赖于节点属性值的写入顺序
执行一次入队操作的步骤顺序是:
    写入新节点的prev为看到的tail节点(保持新节点的next为null);
    对tail执行cas(旧tail值, 新节点);
    最后写入新节点的prev指针所指节点的next指针为新节点
    
⚠️⚠️⚠️    
CLHLock的队列到底是容纳什么的队列??    
队列容纳的是时间线上后面线程看到的锁状态.通过CAS队尾将并发线程排好时间先后.前节点的locked值是当前节点线程看到的锁状态,而当前节点的locked值是当前节点线程希望后节点线程看到的锁状态.
为了保证锁只有一个线程持有,必须保证队列中只有一个锁状态是unlocked其余都是locked,最靠前的线程看到是unlocked,后续线程看到的是locked.最靠前的线程可以持有锁,持有锁的线程在释放锁时需要更新后节点线程看到的锁状态
假如CLHLock支持节点取消,那么持有锁的线程在释放锁时需要更新有效后节点(取消了的节点当作时间线上消失)线程看到的锁状态.(AQS的SIGNAL指的是后面有节点需要唤醒)
其实,就是将一个锁状态变成了按时间线呈现的多个副本.
```

```text
AQS中的节点有prev/next2个指针.prev组成的队列才是AQS的主要依靠点.
prev队列需要时时刻刻都能保证队列的准确性(改动节点时要特别注意);next队列则没有十分准确.

优化写入方法方法保证一个CAS完成节点在队尾入队(即新的尾节点设置好时,新节点的prev和prev.next都设置好了),不会在入队时发生race问题.

AQS节点的next有如下操作:
1. 节点入队同步队列时,为前节点设置next(cas-tail可以保证不会出现并发设置同一个节点的next值)
2. 节点线程获得锁后,将自己节点设置为head后,再将自己的prev节点(即旧头节点)的next设置为null来帮助gc(线程节点按前后顺序依次出队,由于每个节点的prev都是不同的,也不存在并发写入next)
3. 在同步队列节点线程挂起自己前,修复prev队列中自己节点(状态正常)前的已经取消的节点(prev队列中正常节点前的取消节点集合是不会重叠的,这里也不存在并发写入问题)
4. 取消节点时,将自己节点的next设置为自己(这是存在自己节点线程和next节点线程同时写入的问题, todo 待解释)


next干什么的??后续补充



循环不变式invariant
node.next==null时node是队尾节点的,node.next!=null时node在队列节中间.
```




## AbstractQueuedSynchronizer

### 核心方法-不支持中断
#### acquire(独占模式)(是否支持中断取决于tryAcquire)

一定能获得锁资源,那怕是通过挂起等待(不支持中断).

⚠️⚠️⚠️注意:
1. tryAcquire成功的线程不用进入同步队列.
2. 非节点线程与队列中的

```text
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

#### tryAcquire(独占模式)

该方法需要子类实现.
该方法不能阻塞线程,仅仅是简单的根据state字段(并不会也不应该考虑aqs的同步队列的状态)判断并通过CAS完成资源的抢占.这里体现出了try的意义.

Attempts to acquire in exclusive mode. This method should query if the state of the object permits it to be acquired in the exclusive mode, and if so to acquire it.


#### addWaiter(不支持中断)

一定能向队列尾部成功插入一个node.(不支持中断)


AQS的内部维护一个FIFO的双向链表队列,新线程节点追加到队尾
这个队列永远不会让真正的线程节点成为头节点,头节点永远是一个逻辑上dummy-node(参见acquireQueued方法)

⚠️瞬时的临界问题(最新的同步反向队列和旧的同步正向队列)需要上层方法注意并处理.

```text
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    
    // 先尝试enq的简化版(一次CAS将当前节点加入队尾)
    if (pred != null) { // 如果pred==null,意味着tail==null,此时同步队列还未初始化,需要用enq来完成
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node; // 先设置prev,cas完成设置tail后再设置next,保证同步prev队列(次优保证同步next队列)
            return node;
        }
    }
    // 失败了再尝试enq的完整版(通过循环来完成CAS操作,将当前节点加入队尾)
    enq(node);
    return node;
}


// 使用了优化的入队算法,通过先设置node.prev,再CAS tail指针,CAS成功后再设置node.prev.next值,保证了入队的线程安全; 通过CAS head保证初始化线程安全.
private Node enq(final Node node) {
    for (;;) {  // 通过不断循环一定要将node插入到队列的尾部
        Node t = tail;
        if (t == null) { // 队列未初始化
            if (compareAndSetHead(new Node())) // CAS先往队列中插入一个dummy head(队列中当前节点的waitStatus需要承载后续线程节点(不一定必须是紧挨的下个节点)需要被通知的信息,所以需要这样一个dummy-node)
                tail = head; // 注意: CAS先tail后head,会让其他线程看到:head!=null但tail==null
        } else { // 队列已经初始化
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}






注意同步队列在尾部添加节点时:
线程节点先设置prev再CAS设置tail指针,最后设置next.(不用设置head指针)
    在CAS设置tail指针前设置prev成功后,同步反向队列和同步正向队列都是旧的,未变化.(此时tail还是旧值,无法找到未来的新尾节点)
    在CAS设置tail指针成功后且还未设置next前,同步反向队列是新的完整的,同步正向队列中还是旧尾节点(不完整的)
    在CAS设置tail指针后设置next成功后,同步反向队列和同步正向队列都是新的
初始化节点先设置head再设置tail
    初始化设置dummy-node head,指的场景是一个线程获得了锁,同一个时间所有的其他线程因争抢锁失败来初始化队列
    CAS保证了这些线程中只有其中一个设置dummy-node head成功并初始化好,其他线程只能通过else逻辑在初始化节点后依次追加
    dummy-node head的操作会让其他线程看到:head!=null但tail==null,
        按照现有代码设计, 独占模式下,
            获得锁的线程在unlock-tryRelease时只CAS设置state,根本不管同步队列的情况
        按照现有代码设计, 共享模式下,获得锁的线程在unlock-tryReleaseShared时通过循环CAS设置state,然后唤醒后续节点(这里需要考虑发生missed-signal, TODO 待完成 )
            首先如果这个问题存在,那不应该是本方法来解决,可以将存在的这些问题交给更上层的方法来处理.
        加锁时,非节点线程在tryAcquire在失败后变成节点线程前,非节点线程与同步队列无交集.
        释放锁时,


总结:
    addWaiter能保证一定将线程节点追加到同步队列队尾
    同时存在瞬时的临界问题
        同步队列在初始化时,其他线程看到:head!=null但tail==null
        同步队列初始化后,添加线程节点时,其他线程看到最新的同步反向队列和旧的同步正向队列(瞬时后更新).
    临界问题应该有上层方法来处理,addWaiter及其子方法enq不应该处理
```

#### acquireQueued(不支持中断)

一定能获得锁资源,那怕是通过挂起等待(不支持中断).
但该方法可以通过返回值来表明挂起等待过程中是否曾被中断过.

为了防止cpu空转(避免忙等),只有第一个线程节点(head的下一位)才被允许通过cas来争夺锁资源,
其他的node需要通过park将自己的线程挂起,等待被通知后才能恢复线程调度.

关于中断
如果整个过程中没有发生过挂起等待,则返回值不准确,可以看作是方法刚结束时发生了中断而来不及记录

退出该方法后,线程节点变成了逻辑上的dummy-node,不再是线程节点了.

```text
boolean acquireQueued(final Node node, long arg)
    Acquires in exclusive uninterruptible mode for thread already in queue. Used by condition wait methods as well as acquire.
    Returns: true if interrupted while waiting(只计算因为park导致waiting过程中发生的中断)
    

final boolean acquireQueued(final Node node, long arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        
        // 只有当当前节点变为第一个线程节点才能退出这个循环,其余时间只能挂起等待(中断也仅仅是提前唤醒线程,如果此时还不是第一个线程节点,会接着挂起等待)
        for (;;) {
            final Node p = node.predecessor();
            
            // 第一个线程节点的前驱节点是dummy-node,tryAcquire成功后,将第一个线程节点设置为head(初始化的dummy-node不存在了, 此时thread/prev都设置为null)
            // 第一线程释放锁资源后,仅仅将head.waitStatus清空默认值0,并没有改动队列的结构(此时head的thread/prev/waitStatus都默认值,类似于dummy-node)
            // 不管是第几个线程释放完锁资源,head都是一个逻辑上dummy-node(可能不是同一个对象了)
            if (p == head && tryAcquire(arg)) { // 前驱是head时,即当前节点是第一个线程节点,才尝试获取锁
                setHead(node); // 获取锁资源成功设置当前节点成为head(thread/prev都设置为null)(初始化的dummy-node不存在了)(保留了waitStatus,因为后续节点还要用)
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            
            // 因为tryAcquire失败,需要将前驱节点的waitStatus设置为SIGNAL,这样在当前线程挂起后,其他线程看到这个信息才会unpark当前线程
            // unpark/interrupt激活线程后,继续for循环去tryAcquire直到成功
            if (shouldParkAfterFailedAcquire(p, node) && // 检查是否需要挂起
                parkAndCheckInterrupt()) // interrupt导致提前退出park方法,及时清除中断位才能让下次park挂起线程
                interrupted = true; // 因为中断位被清除,需要用独立变量来记录一下
        }
    } finally {
        // 由于支持tryAcquire抛出异常,所以存在failed==true的场景. 当发生抛出异常时,意味着此后不要需要这个节点了,需要移除
        if (failed)
            cancelAcquire(node);
    }
}
```

#### shouldParkAfterFailedAcquire(不支持中断)

This is the main signal control in all acquire loops.
Checks and updates status for a node that failed to acquire. Returns true if thread should block.

⚠️当前方法是acquire-loop的核心控制节点,用来控制抢占锁失败的节点线程是否应该挂起.
⚠️这里是设置前驱节点waitStatus为SIGNAL的唯一地方

决定一个抢占锁失败的线程是否应该进入挂起状态.
按照整体设计,要保证在prev队列中,离当前节点最近的正常前节点的waitStatus为SIGNAL,才能挂起.
因为节点支持取消操作(超时/中断),所以一定是正常前节点

在设置的过程中顺便把紧邻的取消节点清除掉.
node.waitStatus=SIGNAL指的是node节点后有节点线程需要唤醒.(node节点后指的并不一定是下一个节点)

```text
确保前驱节点的waitStatus设置为SIGNAL(即告知其他线程在释放锁资源时记得唤醒当前线程),否则一直返回false让acquire-loop接着尝试以确保前驱节点的waitStatus为SIGNAL.

// 当前方法都是被acquire类方法调用的,所以是同步队列的节点,waitStatus不会有CONDITION
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        // 前节点被设置了SIGNAL,就肯定能被其他线程唤醒,可以安心挂起
        return true;
        
    
    if (ws > 0) { // 只有CANCELED是大于零的.
        // ⚠️穿插实现队列的自我管理(先设置prev再设置pred.next,保证整体设计中prev队列的实时完整性,next队列并不非实时正确) 因为没有使用排他锁,如果发生了新的取消操作,就会出现有取消节点没有清理(最终会收敛到全部被清除)
        // 将当前正常节点与最近的前正常节点之间的取消节点清理掉(由于2个正常节点间的节点集合不存在交集,所以并发执行这段while代码也不会并发问题)
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
        
        // if(ws > 0)这段逻辑就是为了在循环中穿插实现队列的自我管理,不能挂起线程
        return false;
    } else { // 此时只能是0/PROPAGATE.
        // ⚠️ 这里是设置前驱节点waitStatus为SIGNAL的唯一地方
        // waitStatus==0表示还未被设置过,可放心被设置为SIGNAL,以便让其他线程看到后唤醒本线程; waitStatus==PROPAGATE表示可以被设置为SIGNAL,以便让其他线程看到后唤醒本线程(TODO 待补充)
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL); 
        
        // cas如果失败,只可能是pred正在取消等待
        // cas可能成功也要在下一轮中再抢一次锁(毕竟挂起后立马被唤醒cpu开销太大)
        return false;
    }
}

挂起的整体思路是:
在一轮循环中先对pred.waitStatus为Node.SIGNAL,再在下一轮循环中确认.
至于清除prev队列的取消节点,这个是为了队列的自我管理,需要穿插在循环中来做,与方法主体无关,自然不能返回true.
```
#### cancelAcquire(不支持中断)

cancelAcquire只会被acquire类方法调用

一句话总结：
    当线程在尝试获取锁时（acquireQueued 过程中）发生了抛出异常，它需要调用 cancelAcquire 将自己从AQS的同步队列中安全地移除。
也就是说，这个方法是 "退出排队" 的安全逻辑。


⚠️ signal是在前驱节点上设置,而取消是在自己节点上设置

```text
Cancels an [ongoing] attempt to [acquire].
ongoing指的是线程对应的节点已经在同步队列中.



// 只有节点的自己线程才能设置CANCELLED,所以入参node是正常节点
private void cancelAcquire(Node node) {
    if (node == null)
        return;

    // 队列中节点的thread只有在取消时会设置为null.
    node.thread = null;

    // prev队列中,每个正常节点前的无效节点集合是无交集的.所以prev队列在清理取消节点时能并发安全,且维护了prev的实时准确性.  (注意: 这里没有着急重新设置next的值)
    // prev队列的自我管理穿插在取消操作中很合理 
    Node pred = node.prev;
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev;

    // 在node.waitStatus设置为Node.CANCELLED之前,提前做一个volatile-read是为了后面CAS操作以及通过CAS的结果来判断是否发生了并发
    Node predNext = pred.next;

    // ⚠️ signal是在前驱节点上设置,而取消是在自己节点上设置
    // 将当前节点标记为“已取消”
    
    // ⚠️注意英文注释: Can use unconditional write instead of CAS here. After this atomic step, other Nodes can skip past us.Before, we are free of interference from other threads.(在这个原子步骤之后其他节点可以跳过我们,之前我们不受其他线程的干扰(因为用的是prev))
    node.waitStatus = Node.CANCELLED;

    // ⚠️ 在取消等待时,第一线程节点需要unpark后续节点,中间节点保证标记好前节点为signal,尾节点不需要标记signal也不需要unpark后续节点

    // (node == tail)表明当前node很有可能是尾节点,cas-tail确保node为尾节点
    if (node == tail && compareAndSetTail(node, pred)) {
        // 尾节点的next需要设置为null, 但是不能无脑直接设置next为null,存在新节点同时入队的情况.(节点入队时,在cas-tail已经通过对象初始化提前设置为null了,所以cas-tail后设置next属性需要cas的方式来设置)
        compareAndSetNext(pred, predNext, null);
        // 如果此时没有新节点入队,那么compareAndSetNext(pred, predNext, null)成功,此时pred就是新尾节点(不存在unpark后续节点的问题); 如果失败则又有新节点入队,即新节点将pred.next设置为新节点,当前线程就不能无脑设置null了(存在unpark后续节点的问题)
    } else {
        int ws;
        // (pred != head )表示当前线程节点位于中间(不是尾节点,也不是第一个线程节点),需要保证前节点设置好了SIGNAL,这样后续节点才可能被唤醒
        // (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))展示了waitStatus的单向变更
        // 由于取消操作中的先设置thread再设置waitStatus的顺序,在这里被利用起来了.
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL ||
             (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
            pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0) // (next != null)利用循环不变式可以认为此时当前节点不是尾节点
                compareAndSetNext(pred, predNext, next); // 不能无脑设置,存在并发要用cas
                // 如果存在并发操作,不仅有并发取消操作,还有并发的prev队列自我管理(shouldParkAfterFailedAcquire)
        } else {
            unparkSuccessor(node); // 托底,将后面节点的线程唤醒,唤醒后的线程会按照其他方法的既定算法正确执行 (第一个线程节点/tail-CAS失败都会走这里)
            
        }

        node.next = node; // next设置为null也能帮助gc,可是这里选择了这种方式来帮助gc,是为了维护循环不变式:当node.next!=null时该node在prev队列中.
    }
}


感觉:
取消节点时,仅仅设置waitStatus为CANCELLED,再无脑unparkSuccessor一次就行了,至于取消节点由其他方法来回收


如果node是第一线程节点,后面挂起的线程还指望node线程来唤醒它然后它去抢占锁资源呢,node取消时一定要unparkSuccessor.
如果node是尾节点且没有新增后续节点,因为没有后续节点需要唤醒,那么node取消后就不用unparkSuccessor.
如果node是中间节点,只需要保证node.prev.waitStatus=SIGNAL就行,因为node前面正持有锁的线程在release时会唤醒node后的等待节点.
    因为发生并发操作,所以设置node.prev.waitStatus=SIGNAL要用CAS;
    如果CAS失败,必须再唤醒一个后续节点线程.
        要保证正常节点线程至少有一个是非挂起状态,多激活几个节点线程也无所谓,不能少了.
        并发情况比较复杂,参与的线程可能是release,也可能是cancelAcquire,甚至是队列自管理的,直接使用unparkSuccessor来简化操作.
```

等价转换后更清晰
```text
private void cancelAcquire(Node node) {
    // Ignore if node doesn't exist
    if (node == null)
        return;

    // 队列中节点的thread只有在取消时会设置为null. 取消操作中,先设置thread再设置waitStatus
    node.thread = null;

    // Skip cancelled predecessors
    // prev队列中,每个正常节点前的无效节点集合是无交集的.所以prev队列在清理取消节点时能并发安全,且维护了prev的实时准确性. 
    // prev队列的自我管理穿插在取消操作中很合理
    // 注意: 这里没有着急重新设置next的值
    Node pred = node.prev;
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev;

    // predNext is the apparent node to unsplice. CASes below will fail if not, in which case, we lost race vs another cancel or signal, so no further action is necessary.
    // ⚠️注意英文注释: 在设置CANCELLED之前提前读取pred.next值来,造成predNext happens-before (node.waitStatus == Node.CANCELLED)
    // .next的修改只在(cancel/cancel 和 cancel/signal)
    Node predNext = pred.next;

    // ⚠️ signal是在前驱节点上设置,而取消是在自己节点上设置
    // cancelAcquire只会被acquire类方法调用,且只有tryAcquire会发生异常,此时当前线程还未从/已经调用过shouldParkAfterFailedAcquire(即此时当前线程从未/已经设置过SIGNAL)
    // 将当前节点标记为“已取消”(TODO 会不会因为写入CANCELLED而覆盖本节点上的SIGNAL值???)
    
    // ⚠️注意英文注释: Can use unconditional write instead of CAS here. After this atomic step, other Nodes can skip past us.Before, we are free of interference from other threads.(在这个原子步骤之后其他节点可以跳过我们,之前我们不受其他线程的干扰(因为用的是prev))
    node.waitStatus = Node.CANCELLED;

    // ⚠️ 在取消等待时,第一线程节点需要unpark后续节点,中间节点保证标记好前节点为signal,尾节点不需要标记signal也不需要unpark后续节点

    // If we are the tail, remove ourselves.
    if (node == tail && compareAndSetTail(node, pred)) {
        compareAndSetNext(pred, predNext, null);
        return;
    }
    
    // If successor needs signal, try to set pred's next-link so it will get one. Otherwise wake it up to propagate.
    int ws;
    if (pred != head &&
        ((ws = pred.waitStatus) == Node.SIGNAL ||
         (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
        pred.thread != null) {
        Node next = node.next;
        if (next != null && next.waitStatus <= 0){
            compareAndSetNext(pred, predNext, next);
        }
        node.next = node; // help GC    
        return;
    }
    
    unparkSuccessor(node);
    node.next = node; // help GC
}
```

cancelAcquire方法导致同步队列的节点结构变化如下图:
![](E:\git-repo\cjh-repo\java-lang-spec-learn\docs\images\juc-aqs-syn-queue.png)

我们对于CANCELLED节点状态的产生和变化已经有了大致的了解，但是为什么所有的变化都是对Next指针进行了操作，而没有对Prev指针进行操作呢？什么情况下会对Prev指针进行操作？
```text
执行cancelAcquire的时候，当前节点的前置节点可能已经从队列中出去了（已经执行过Try代码块中的shouldParkAfterFailedAcquire方法了），如果此时修改Prev指针，有可能会导致Prev指向另一个已经移除队列的Node，因此这块变化Prev指针不安全。 
shouldParkAfterFailedAcquire方法中，会执行下面的代码，其实就是在处理Prev指针。shouldParkAfterFailedAcquire是获取锁失败的情况下才会执行，进入该方法后，说明共享资源已被获取，当前节点之前的节点都不会出现变化，因此这个时候变更Prev指针比较安全。
```


#### unparkSuccessor(不支持中断)

unparkSuccessor必定能保证node后最近的一个节点(如果存在的话)线程恢复调度.

````text
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    if (ws < 0) // ws只可能是SIGNAL/PROPAGATE
        compareAndSetWaitStatus(node, ws, 0); // (设置为0是为了说明已经开始处理,清空标识)

    Node s = node.next;

    // (s == null)则node此时是尾节点(可能马上就有新的尾节点),(s.waitStatus > 0)则node.next节点是取消节点(终态不再改变)
    // 上面2种情况都要按prev队列从后往前找到离node最近的正常节点
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0) // 表示t节点是正常节点(没有发生取消操作)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}



最终选中一个按prev队列从后往前找到离node最近的正常节点(此时s.waitStatus <= 0)
    从(s.waitStatus <= 0)到LockSupport.unpark(s.thread)这个间隙,
        如果节点s没发生cancelAcquire,unpark让正常节点线程恢复调度重新抢占锁(此时没浪费unpark)
        如果节点s发生了cancelAcquire,如果s.thread不去唤醒后续节点让其抢占锁,会导致存在永远挂起的线程(浪费了unpark)

unparkSuccessor能保证node后最近的一个节点(如果存在的话)线程恢复调度,这里仅仅只关注恢复线程的活动能力,至于这个线程接下来该干什么,再其他方法中会定义他们的行为.



(waitStatus > 0)则表示节点已经不再参与锁竞争,无需额外的唤醒.
    通常waitStatus设置为1都是该节点线程自己操作的,线程在设置完状态后,处理好其他操作后就结束线程了.再unpark就无意义.
 
````


#### tryAcquireNanos(支持超时)

```text
public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
        
    return tryAcquire(arg) ||
        doAcquireNanos(arg, nanosTimeout);
}
```

#### doAcquireNanos(支持超时)

```text
private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (nanosTimeout <= 0L)
        return false;
        
    final long deadline = System.nanoTime() + nanosTimeout;
    // 线程节点入队同步队列
    final Node node = addWaiter(Node.EXCLUSIVE);
    
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            // 只有第一个节点线程才能去抢占,后面的节点线程需要等待
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return true;
            }
            
            // 如果超时退出循环并返回false表示抢占锁失败
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L)
                return false;
                
            if (shouldParkAfterFailedAcquire(p, node) &&
                nanosTimeout > spinForTimeoutThreshold) // 如果需要等待的时间太短就通过for循环来完成耗时比较划算,通过park(timeout)来完成耗时不划算
                LockSupport.parkNanos(this, nanosTimeout);
            
            // 在park前中后都有可能发生中断
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    } finally {
        // 超时和中断都可能引起退出该方法.在退出前将线程节点从同步队列移除
        if (failed)
            cancelAcquire(node);
    }
}
```


#### release(不明确是否支持中断)(独占模式)

必定能唤醒一个prev队列最靠前的节点线程(如果存在的话).
由于是独占模式,不存在并发release,所以同时清空头节点的waitStatus为0

```text
public final boolean release(int arg) {
    // 看tryRelease是否支持中断
    if (tryRelease(arg)) {
        // 为了FIFO,唤醒头节点后最近的一个正常节点线程
        Node h = head;
        
        if (h != null && h.waitStatus != 0) 
            unparkSuccessor(h); // 按prev队列从后往前找一个离h最近的正常节点线程,并唤醒.
        return true;
    }
    return false;
}

// 为什么要h.waitStatus != 0
(h.waitStatus != 0)表示节点h后有需要唤醒的节点线程.
    最坏的情况是,刚判断完(h.waitStatus != 0)然后在unparkSuccessor前,prev队列的后面节点都取消了,此时也就没有要唤醒的线程,等价于唤醒了一个节点线程.

节点取消并不会修改prev.waitStatus,仅仅修改自己节点的waitStatus.
```

#### tryRelease(不明确是否支持中断)

Attempts to set the state to reflect a release in exclusive mode.


#### fullyRelease(独占模式)(不支持中断)

```text
final int fullyRelease(Node node) {
    boolean failed = true;
    try {
        int savedState = getState();
        if (release(savedState)) { // 使用独占模式的release
            failed = false;
            return savedState;
        } else {
            throw new IllegalMonitorStateException();
        }
    } finally {
        if (failed)
            node.waitStatus = Node.CANCELLED; // 锁状态异常会导致条件节点变为取消节点
    }
}
```

### 工具方法

#### hasQueuedPredecessors(不支持中断)

判断当前线程是否是第二个节点(即第一线程节点)
除去队列中的第一个节点是dummy-node,后续节点都是线程节点/取消节点



```text
public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized before tail and on head.next being accurate if the current thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order (与队列初始化反方向读取,这样后面的h!=t才能正确表示队列中有线程节点)
    Node h = head;
    Node s;
    return h != t &&
        ((s = h.next) == null || s.thread != Thread.currentThread());
}

(h != t)成立时,队列已经初始化,且有线程节点
(h.next!=null)成立时,s在队列中;

((s = h.next) == null || s.thread != Thread.currentThread())是作者一个惯用的写法,他想表达的是 !(h.next!=null && s.thread==Thread.currentThread()) 即不存在第一线程节点是当前线程的


h != t && ((s = h.next) == null || s.thread != Thread.currentThread())的意思就是,此时队列的第一线程节点是否当前线程的
```

#### isOnSyncQueue

入参的node初始位于条件队列中.

Returns true if a node, always one that was initially placed on a condition queue, is now waiting to reacquire on sync queue.

```text
final boolean isOnSyncQueue(Node node) {
    if (node.waitStatus == Node.CONDITION || node.prev == null)
        return false;
    if (node.next != null) // If has successor, it must be on queue
        return true;
    /*
     * node.prev can be non-null, but not yet on queue because the CAS to place it on queue can fail. So we have to traverse from tail to make sure it actually made it.  
     * It will always be near the tail in calls to this method, and unless the CAS failed (which is unlikely), it will be there, so we hardly ever traverse much.
     */
     // 反向遍历同步队列,确定当前节点是否在同步队列中(处理)
    return findNodeFromTail(node);
}


node.prev == null 
    表示该节点还未链接到同步队列(因为同步队列存在逻辑上的dummy-node节点,所以必然同步队列中的所有线程节点的prev都非null,而并非所有的线程节点的next都有值,比如线程节点是尾节点)
node.next != null
    如果next不为空,说明该节点已经在同步队列中(因为条件队列中节点是不设置next)   
    
return findNodeFromTail(node)   
    如果上面两种情况都不确定（prev != null 且 next == null）,就说明节点可能刚刚在 signal() 转移过程中 
```

#### transferAfterCancelledWait


Transfers node, if necessary, to sync queue after a cancelled wait. (将等待节点转移到同步队列中)
Returns true if thread was cancelled before being signalled.(如果转移)

```text
final boolean transferAfterCancelledWait(Node node) {
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
        enq(node);
        return true;
    }
    /*
     * If we lost out to a signal(), then we can't proceed until it finishes its enq().  Cancelling during an incomplete transfer is both rare and transient, so just spin.
     */
    while (!isOnSyncQueue(node))
        Thread.yield();
    return false;
}
```


### 核心方法-支持中断

#### acquireInterruptibly(独占模式)(支持中断)

acquire方法的支持中断版本

```text
public final void acquireInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (!tryAcquire(arg)) 
        doAcquireInterruptibly(arg); // 尝试失败则通过等待队列完成
}
```

#### doAcquireInterruptibly(独占模式)(支持中断)

acquireQueued方法的支持中断版本.直接抛出中断异常而非以返回值告知调用方

```text
private void doAcquireInterruptibly(int arg) throws InterruptedException {
    final Node node = addWaiter(Node.EXCLUSIVE);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt()) // 如果当前节点的前节点标记好了SIGNAL后才能挂起当前线程
                throw new InterruptedException(); // 如果当前线程是因为中断才退出park的话,直接抛出中断异常
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```