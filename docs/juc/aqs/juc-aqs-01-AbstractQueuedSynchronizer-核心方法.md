# juc-AQS-方法注释-AbstractQueuedSynchronizer-核心方法


```text
挂起（Suspend)和阻塞（Block)的描述层次就不同:
    当站在锁的设计者角度,当调用park时,可以用挂起来表述;
    当站在锁的使用者角度,当因为方法内部使用了park方法导致当前线程无法继续执行时,用阻塞来表述.
```

```text
⚠️ AQS是无锁同步器,无锁同步器底层主要依靠:
1. 仅有CAS能保证test-and-set的原子性,任意2个语句都存在并发竞态问题.
2. CAS的原子性是保证算法正确的一个重要依靠支点;CAS/volatile的内存读写屏障也是保证算法正确的另一个重要的依靠支点.
```


```text
⚠️AQS要解决的核心问题:
1. CLH-lock中的线程都是一直主动spin自旋轮询前节点的status,所以能立马感知到前节点status的改变;而且节点也不支持超时/中断等引发的取消操作.整个算法可以放心地依靠前节点释放锁的操作必定能被后节点感知到.
2. 如果让CLH-lock支持超时/中断引发的取消操作,仅仅需要改动一点点:将原来判断紧挨的前节点的status状态 改为 跳过取消的节点,判断最近的正常前驱节点的status状态.
3. AQS基于CLH-lock算法,并在此基础之上新增了支持超时/中断引发的取消等待,并支持线程通过挂起/唤醒线程机制来等待(而非自旋忙等待).
4. AQS没有选择让线程spin忙等待而是挂起等待,虽然这样节省了cpu开销却让线程在挂起等待时缺失了感知前驱节点的status状态改变的能力.这就要求前驱节点在结束占用锁资源后通过结束后驱节点的挂起等待来恢复感知能力.
5. ⚠️AQS由于node节点唤醒node.next节点的操作与node.next节点的超时/中断引发的取消操作可能会同时发生,存在并发竟态问题.导致前节点无法准确确定该唤醒哪个后面节点(可能刚决定了一个后面节点,它却同时发生了超时/中断引发的取消操)
6. ⚠️解决这种存在的并发问题的暴力简单思路是模仿CLH-lock,让前驱节点唤醒后面所有的节点.
7. AQS选择了更加精细的操作,唤醒一个或者部分节点线程.
```



```text
AQS基于CLH的lock-free queue-based Lock算法.同时又使用优化写入方法实现一个cas完成节点入队.

CLH是基于一个FIFO的队列.
    CLH节点只有一个prev指针;
    CLH队列只有head/tail 2个指针.
CLH节点的status
    只能后节点设置和自己节点退出时设置.
    status值变化是单向的.


优化写入方法依赖于节点属性值的写入顺序
执行一次入队操作的步骤顺序是:
    写入新节点的prev为看到的tail节点(保持新节点的next为null);
    对tail执行cas(旧tail值, 新节点);
    最后写入新节点的prev指针所指节点的next指针为新节点

    
⚠️⚠️⚠️CLHLock的队列到底是容纳什么的队列??    
1. 队列节点的先后顺序容纳的是并发线程抢夺锁的优先顺序,在前节点释放锁前后节点不能抢占锁;(核心功能)
2. 队列节点的locked字段容纳的是线程对锁的持有状态:线程是否已经释放锁了,还是说正在等待或者已经持有锁;(核心功能)
3. 队列节点的thread字段容纳的是线程与其节点的映射关系(辅助功能)
```

```text
AQS中的节点有prev/next2个指针.prev字段是用来构成FIFO同步队列,而next字段只是尽量避免通过tail+prev反向遍历的一种优化手段.
代码中特别注意对prev字段的改动,保证了prev队列时时刻刻都准确;而next字段就不那么可靠了.

优化写入方法方法保证一个CAS完成节点在队尾入队(即新的尾节点设置好时,新节点的prev和prev.next都设置好了),不会在入队时发生race问题.

AQS节点的next有如下操作:
1. 节点入队同步队列时,为前节点设置next(cas-tail可以保证不会出现并发设置同一个节点的next值)
2. 节点线程获得锁后,将自己节点设置为head后,再将自己的prev节点(即旧头节点)的next设置为null来帮助gc(线程节点按前后顺序依次出队,由于每个节点的prev都是不同的,也不存在并发写入next)
3. 在同步队列节点线程挂起自己前,修复prev队列中自己节点(状态正常)前的已经取消的节点(prev队列中正常节点前的取消节点集合是不会重叠的,这里也不存在并发写入问题)
4. 取消节点时,将自己节点的next设置为自己(为了维护invariant)

```

```text
识别不变量：找出一个或多个在锁算法执行过程中始终为真的条件。这是最关键的一步。
基于不变量进行证明：
    互斥：证明锁的不变量蕴含着“不可能有两个线程同时持有锁”。
    无死锁/无饥饿：证明任何一个试图获取锁的线程，最终都能成功。这通常需要证明存在一个“先后顺序”或“等待链”，并且这个顺序是有限的。


⚠️⚠️⚠️AQS在维护的循环不变式invariant

node.prev在入队后,除非是头节点否则一定是非null.(节点取消后仍保持node.prev非null)
node.next在入队后,除非是尾节点否则一定是非null(节点取消后仍保持node.next==node)
node.thread从Node对象实例化后,除非成为头节点或者发生取消操作,一直保持非null且不变.

注意: 
node.prev/thread值都是node节点线程自己设置的(非其他线程).
node.next值会被其他线程设置.
```

```text
由于并发下有无穷的执行顺序,穷举法不好使.衍生出了通过Invariant+反证法来证明锁在并发下的正确性.

💯AQS在维护的循环不变式invariant
1. 队列的head 和 tail 指针在初始化后永远不会为 null.
        初始化时，它们指向一个虚拟节点.
2. 入队的原子性
        先设置prev,再cas-tail,再prev.next,保证不会因为入队导致看到状态不一致的prev同步队列.
3. 前驱节点的不变性
        prev指针在节点入队后永远不会改变(除非: 成为头节点时变为null)(节点取消后仍保持node.prev非null)
4. node.next在入队后,除非是尾节点否则一定是非null(节点取消后仍保持node.next==node)      
5. node.thread从Node对象实例化后,除非成为头节点或者发生取消操作,一直保持非null且不变.  
6. head节点的特殊性
        head节点是一个虚拟节点(或刚刚释放锁的节点)。它不代表正在等待的线程。真正等待的线程是从 head.next 开始的
7. 挂起前的检查   
        一个线程在挂起自己之前，必须重新检查它是否能够获取资源并确保前节点被标记了SIGNAL
8. 唤醒的可靠性
        当一个线程释放资源时，它必须检查队列并唤醒一个符合条件的后继节点
        
        
注意: 
node.prev/thread值都是node节点线程自己设置的(非其他线程).
node.next值会被其他线程设置.        
```


```text
node.waitStatus == CANCEL  表示node节点已取消,请忽略该节点的存在
node.waitStatus == 0       表示node节点后面[不确定有没有]挂起等待的线程待唤醒
node.waitStatus == SIGNAL  表示node节点后面[有]挂起等待的线程待唤醒
```

## 核心方法

AQS要解决的核心问题就是unparkSuccessor/cancelAcquire的并发问题.
可以多余地unpark挂起的线程,不能少unpark挂起的线程.


### unparkSuccessor(不支持中断)

unparkSuccessor必定能保证node后最近的一个节点(如果存在的话)线程恢复调度.
注意:可能unpark一个正常发生取消操作的节点线程.

````text
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    if (ws < 0) // 如果node是取消节点就不要再改动waitStatus,其他情况下ws只可能是SIGNAL/PROPAGATE
        compareAndSetWaitStatus(node, ws, 0); // (设置为0是为了说明已经开始处理,清空标识)

    Node s = node.next;

    // (s == null)则node此时是尾节点(要防止遗漏可能马上就有的新尾节点); (s.waitStatus > 0)则node.next节点是不需要unpark的取消节点(要防止遗漏node.next后还有在等待的节点)
    // 上面2种情况都要按prev同步队列从后往前找到离node最近的正常节点
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0) // 表示t节点是正常节点(没有发生取消操作)
                s = t;
    }
    
    // 备注: 从判断s.waitStatus正常到unpark这个间隙,s.waitStatus可能有变
    if (s != null)
        LockSupport.unpark(s.thread);
}


(waitStatus > 0)则表示节点已经不再参与锁竞争,无需额外的唤醒.

最终选中一个按同步队列从后往前找到离node最近的正常节点(此时s.waitStatus <= 0)
    从(s.waitStatus <= 0)到LockSupport.unpark(s.thread)这个间隙,
        如果节点s没发生cancelAcquire,unpark让正常节点线程恢复调度重新抢占锁(此时没浪费unpark)
        如果节点s发生了cancelAcquire,如果s.thread不去唤醒后面等待的节点线程,会导致存在永远挂起的线程(浪费了unpark)

unparkSuccessor能保证node后最近的一个节点(如果存在的话)线程恢复调度,本方法只关注恢复线程的活动能力,其他问题留在其他方法来解决.
````


### cancelAcquire(不支持中断)

cancelAcquire只会被acquire类方法调用.

Cancels an ongoing attempt to acquire. ongoing指的是线程对应的节点已经在同步队列中.

⚠️ signal是在前驱节点上设置,而取消是在自己节点上设置

```text
// 只有节点的自己线程才能设置CANCELLED,所以入参node是正常节点
private void cancelAcquire(Node node) {
    if (node == null)
        return;

    // 节点的thread只有在取消/成为头节点时会设置为null.
    node.thread = null;


    // 此时node还是正常状态. 在prev同步队列中每个正常节点前的无效节点集合是无交集的.所以prev队列在清理取消节点时能并发安全.  (注意: 这里没有着急重新设置next的值) (prev队列的自我管理穿插在取消操作中很合理)
    Node pred = node.prev;
    while (pred.waitStatus > 0)
        node.prev = pred = pred.prev;

    // 此时node还是正常状态,正常节点的prev.next是不会被改变,只有取消节点和尾节点的next才会被改变
    Node predNext = pred.next;

    // --------------------------------------------- cancelAcquire方法分2段逻辑:前半段(改动前的快照)和后半段 ------------------------------------------------------------------------------------

    // ⚠️ signal是在前驱节点上设置,而取消是在自己节点上设置 将当前节点标记为“已取消”
    node.waitStatus = Node.CANCELLED;


    // 💯 尾节点不怕遗漏unpark
    // 💯 中间节点只要保证在前节点状态正常时将其设置为CANCELLED(不会遗漏unpark,前面正常节点顶着)
    // 💯 第一线程节点取消时一定要unparkSuccessor(既是为了防止并发其他线程都挂起了,也是为了防止存在竟态问题的release的unparkSuccessor)

    // 当前线程节点是尾节点 (也可以无脑cas-tail来判断,前面的node==tail是一种小优化)
    if (node == tail && compareAndSetTail(node, pred)) {
        // 为了维护invariant:尾节点的next需要设置为null (防止并发所以采用cas-next)
        compareAndSetNext(pred, predNext, null);
    } else {
        int ws;
        // 当前线程节点是中间节点(不是尾节点也不是第一个线程节点)时,后面有待唤醒的线程,需要趁前节点正常时设置为SIGNAL才有可能唤醒后面线程(依赖前面节点负责任的唤醒)
        if (pred != head &&
            ((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&   // 是为了防止前节点有并发(并发cancelAcquire/unparkSuccessor)
            pred.thread != null) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0) // 这里的(next != null)是为了防止next.waitStatus空指针
                compareAndSetNext(pred, predNext, next); // 存在并发要用cas,修复pred.next字段值
        } else {
            unparkSuccessor(node); 
            // 这里才是大道,通过调用unparkSuccessor将可能浪费的unpark传递出去. (前面的if就是极限优化)
            // (即使unpark多余线程也不影响,无非线程唤醒后再次检查条件不允许接着挂起等待. 这里就是javadoc提到的Cancellation introduces some conservatism to the basic algorithms)
        }

        node.next = node; // next设置为null也能帮助gc,可是这里选择了这种方式来帮助gc,是为了维护循环不变式:在同步队列中node.next==null时node是尾节点.
    }
}

------------------------------------------------------------------------------
⚠️⚠️⚠️先说结论: 想要证明cancelAcquire不会浪费unpark很难.


同步队列的自我管理发生在shouldParkAfterFailedAcquire/cancelAcquire,这里仅仅指重组prev队列.

node节点在同步队列中的位置:
1. node节点是尾节点
2. node节点是中间节点
3. node节点是第一线程节点

可能会影响节点node的并发事件:
1. prev节点的cancelAcquire
2. 前节点的unparkSuccessor在node节点处的unpark调用
3. next节点的acquireQueued
4. 本节点node的cancelAcquire
5. 同步队列的自我管理

⚠️讨论的核心议题是: 在执行节点node的cancelAcquire的过程中,如何避免node节点线程被unpark(即前面节点的unparkSuccessor在node节点线程上调用了unpark)导致node后面节点因缺少这个unpark而无法唤醒.
⚠️怎么判断有没有被unpark??? 
    没有办法直接判断当前线程是否被unpark.
    其次unpark是无法叠加的.(在共享模式下,如果多个线程同时唤醒一个要执行取消操作的线程,取消节点执行一次unparkSuccessor是不够的.)


compareAndSetTail(node, pred) 保证了node节点是尾节点,因为后面没有等待唤醒的线程,无论node线程有没有消费unpark都不算浪费unpark. 
(pred.waitStatus==SIGNAL  && pred.thread != null)保证了cancelAcquire(node)发生在cancelAcquire(node.prev)前,unparkSuccessor的unpark最多落在node.prev上,不会落在node节点线程上.
((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) && pred.thread != null 也就是说,前节点是正常时,本节点取消时只要保证前节点线程知道后面有待唤醒的节点就行.
    如此递归到第一线程节点不适用,因为第一线程节点前面没有等待的节点线程了.(pred != head)就是为了这个准备的
(pred != head) 同时,如果其他线程都在等待,第一线程节点此时发生取消操作,必须要唤醒后面的线程,不然这些线程就没有被唤醒的机会了.

else存在的场景都是存在竟态问题的并发cancelAcquire/unparkSuccessor
    比如第一线程节点: 刚释放锁的线程unparkSuccessor了第一线程,同时第一线程也发生了cancelAcquire,由于这2个方法执行都是多step的,无法再优化了,只能为了防止遗漏unpark无脑再unparkSuccessor生成一个供后面使用的unpark.
    比如中间节点:在取消时,发现前节点也发生取消了,无法保证unparkSuccessor的unpark是作用在了前节点线程还是本节点线程,所以为了防止遗漏唤醒也是无脑再unparkSuccessor生成一个供后面使用的unpark.
```






