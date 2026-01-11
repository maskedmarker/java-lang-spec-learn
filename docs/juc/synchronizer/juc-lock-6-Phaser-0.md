# juc-Phaser

```text
Phaser 是一个支持“多阶段（phase）、可动态注册/注销参与方、可分层聚合”的屏障型同步器.
它解决的是：参与线程数不固定、阶段数不固定、同步结构可能呈树形 的协作问题.

Phaser 适用的典型场景
场景一：多阶段任务流水线（最经典）
        问题特征: 任务分为多个阶段(Phase 0/Phase 1/Phase 2 ...); 每个阶段必须等所有参与者完成后才能进入下一阶段;阶段数不预先固定(可选)
        示例: 编译流程：解析 → 优化 → 生成; 批处理任务：加载 → 计算 → 汇总 → 持久化; 并行算法中的迭代步
        
场景二：参与方数量动态变化
        问题特征: 线程不是一开始就全部确定;执行过程中新线程加入协作,已完成任务的线程退出
        示例: Fork/Join风格任务分解, Master/Worker模式,Worker数量变化
场景三：分层(树形)同步结构
        问题特征: 大规模任务,不希望所有线程都竞争同一个屏障,希望“局部先汇聚,再全局同步”
        示例: MapReduce风格计算;分区并行处理;NUMA / 多核亲和任务
        Phaser 可以有 parent,子Phaser先完成→汇聚到父 Phaser,类似并发屏障的树形归约.
场景四：并行仿真/游戏 Tick/时间步推进
        问题特征: 所有参与者必须在“同一时间步”前进,每一步之间有严格的同步点,参与对象可能中途加入/离开
        示例:多线程物理仿真;游戏服务器的帧同步;离散事件模拟
场景五：替代 CyclicBarrier 的增强版用法
        问题特征: 线程数不是固定的,需要多阶段,某些线程完成后不再参与后续同步,希望barrier能“自然结束”
        
Phaser 不适合的场景
一次性等待 (CountDownLatch 更简单,更清晰)
严格固定参与者+简单屏障  (CyclicBarrier 更轻量)
```


```text
A reusable synchronization barrier, similar in functionality to CyclicBarrier and CountDownLatch but supporting more flexible usage.
Registration. 
    Unlike the case for other barriers, the number of parties registered to synchronize on a phaser may vary over time.
Synchronization. 
    Like a CyclicBarrier, a Phaser may be repeatedly awaited. Method arriveAndAwaitAdvance has effect analogous to CyclicBarrier.await.
    Each generation of a phaser has an associated phase number. The phase number starts at zero, and advances when all parties arrive at the phaser, wrapping around to zero after reaching Integer.MAX_VALUE. 

```

```text
Phaser相较于CyclicBarrier:
Phaser支持动态变化的parties,CyclicBarrier的parties是不可变的.

```

## 使用样例



## 源码实现

### Phaser属性

```text
public class Phaser {
        private volatile long state;     // 用一个long同时支持4个维度的状态(terminated-bit是否终结/phase哪个阶段/parties注册的参与者数量/unarrived还未完成的参与者)
        
        private final Phaser parent;     // Phaser支持单根树形层级结构
        
        
        // 只有根节点有evenQ和oddQ,evenQ和oddQ代表Treiber栈的栈顶,用来存储等待的线程               
        // 当phase是奇数时使用oddQ;当phase是偶数时使用evenQ
        // 其他节点共用根节点的evenQ和oddQ;而且其他节点主动同步根节点的phase.
        private final Phaser root;
        private final AtomicReference<QNode> evenQ;    
        private final AtomicReference<QNode> oddQ;
}

parent/root/evenQ/oddQ都是不可变的.
虽然Phaser支持层级树形结构,但是只有根节点的state的phase部分实时准确表示整体的phase.非根节点的state的phase部分不准确,可能存在落后情况.

Phaser使用双队列evenQ和oddQ,phase切换时,旧队列天然“失效”,Prevents cross-phase interference
```


```text
java.util.concurrent.Phaser.getPhase

public final int getPhase() {
    return (int)(root.state >>> PHASE_SHIFT);    // 根节点的phase就代表了整体的phase,所有子节点的phase以根节点的为准.💯💯💯
}
```

### 构造函数

```text
public Phaser(Phaser parent, int parties) {
    // parties最大为2^16
    if (parties >>> PARTIES_SHIFT != 0)
        throw new IllegalArgumentException("Illegal number of parties");
    
    // 没有parent的情况下,phase的初始值是0
    int phase = 0;
    this.parent = parent;
    if (parent != null) {
        final Phaser root = parent.root;               // Phaser树中,parent和当前对象拥有相同的树根.(Phaser树中新增节点时,最初的那个节点就是树根)
        this.root = root;
        
        this.evenQ = root.evenQ;                       // 非根节点共用树根的奇偶Treiber栈 (Treiber stacks for waiting threads)
        this.oddQ = root.oddQ;
        if (parties != 0)
            phase = parent.doRegister(1);              // 💯💯向父节点注册,父节点会返回注册成功的phase. 保持与父节点相同的phase.
    } else {
        this.root = this;                               // Phaser树中只有当前对象,当前对象就是树根
        this.evenQ = new AtomicReference<QNode>();      // 只有根节点才有evenQ和oddQ
        this.oddQ = new AtomicReference<QNode>();
    }
    
    // 正常情况下,初始状态state中的parties和unarrived是相等. 为了表示没有任何已注册参与方的Phaser,使用了一种特殊的非法状态来表示,即state的parties部分为0同时state的unarrived部分为1,也即EMPTY常量
    this.state = (parties == 0) ? (long)EMPTY : ((long)phase << PHASE_SHIFT) | ((long)parties << PARTIES_SHIFT) | ((long)parties);
}

根节点的phase初始值为0,子节点的phase来自父节点的phase.
如果子节点注册时,沿子节点到根节点的路径上的所有节点都没完全到达,那么这条路径上的节点phase就与根节点的phase保持相同.
随着时间推移,非根节点的phase会落后于根节点的phase
```


### 关键字段

```text
state

Primary state representation, holding four bit-fields: 
    unarrived -- the number of parties yet to hit barrier (bits 0-15) 
    parties -- the number of parties to wait (bits 16-31) 
    phase -- the generation of the barrier (bits 32-62) 
    terminated -- set if barrier is terminated (bit 63 / sign) 
Except that a phaser with no registered parties is distinguished by the otherwise illegal state of having zero parties and one unarrived parties (encoded as EMPTY below). 
(为了表示没有任何已注册参与方的Phaser,使用了一种特殊的非法状态来表示,即(parties==0&&unarrived==1), 这个特殊状态需要以整体(int)state(即 parties|unarrived)来使用,不可单独判断

To efficiently maintain atomicity, these values are packed into a single (atomic) long. (为了实现原子性操作,将这些要素嵌入到一个long类型的state中)
Good performance relies on keeping state decoding and encoding simple, and keeping race windows short. (良好的性能取决于保持状态解码和编码的简单性，并缩短竞争窗口。)
All state updates are performed via CAS except initial registration of a sub-phaser (i.e., one with a non-null parent). (除了sub-phaser的初始注册之外,所有状态更新均通过CAS操作来执行)
In this (relatively rare) case, we use built-in synchronization to lock while first registering with its parent. The phase of a sub-phaser is allowed to lag that of its ancestors until it is actually accessed -- see method reconcileState.


state (64-bit int)
+--------------------------+--------------------------------------------------------------+------------------------------+---------------------------+
|  terminated-bit (1-bit)  |          phase (31-bits)                                     |       parties (16-bits)      | unarrived-bit (16-bit)    |
+--------------------------+--------------------------------------------------------------+------------------------------+---------------------------+
                         63|62                                                          32|31                          16|15                        0
                         
phase支持Integer.MAX_VALUE
```

## Node

Wait nodes for Treiber stack representing wait queue (javadoc虽然描述为wait-queue,实际的数据结构是栈,操作也仅仅是push/pop)
无锁Treiber-stack,栈中元素就是Node

wait-queue中的元素主要是:等待当前phase结束的参与者


```text
static final class QNode implements ForkJoinPool.ManagedBlocker {
    final Phaser phaser;
    final int phase;
    final boolean interruptible;
    final boolean timed;
    boolean wasInterrupted;
    long nanos;
    final long deadline;
    volatile Thread thread; // nulled to cancel wait
    QNode next;

    QNode(Phaser phaser, int phase, boolean interruptible, boolean timed, long nanos) {
        this.phaser = phaser;
        this.phase = phase;                     // 进入等待时的phase
        this.interruptible = interruptible;
        this.nanos = nanos;
        this.timed = timed;
        this.deadline = timed ? System.nanoTime() + nanos : 0L;
        thread = Thread.currentThread();
    }

    public boolean isReleasable() {
        if (thread == null)
            return true;
        
        // (QNode.phase与Phaser的phase对比) 进入等待时的phase已经结束了,可以结束等待了
        if (phaser.getPhase() != phase) {
            thread = null;
            return true;
        }
        
        // 支持中断结束等待
        if (Thread.interrupted())
            wasInterrupted = true;
        if (wasInterrupted && interruptible) {
            thread = null;
            return true;
        }
        
        // 支持超时结束等待
        if (timed) {
            if (nanos > 0L) {
                nanos = deadline - System.nanoTime();
            }
            if (nanos <= 0L) {
                thread = null;
                return true;
            }
        }
        
        // 其他情况,请继续保持等待
        return false;
    }

    public boolean block() {
        if (isReleasable())      // 先开始检查,避免不必要的等待
            return true;
        else if (!timed)
            LockSupport.park(this);
        else if (nanos > 0L)
            LockSupport.parkNanos(this, nanos);
        return isReleasable();    // 返回值提示是否继续等待
    }
}
```
