# juc-Exchanger

Exchanger不仅适用于一对线程一次或多次的交换,还适用于不固定的多对线程一次或多次的交换.而且这种交换是双向的,而非单向的(Pipe是单向的传递而非交换).
如下术语方便后续解释,
参与者: 调用Exchanger方法,期望获取到交换的数据的线程都称为参与者
等待着: 先发起exchange的线程,它会等待某个参与者与其交换数据.
releaser: 后发起exchange的线程,与某个等待着发生交换,并终止等待者的等待.

## 使用样例

```java

```



## 源码实现

### 关键字段

```text
private volatile Node slot;

private volatile Node[] arena;

// The index of the largest valid arena position, OR'ed with SEQ number in high bits, incremented on each update. The initial update from 0 to SEQ is used to ensure that the arena array is constructed only once.
// arena数组在初始化后,并不是将所有的槽位开放,而是动态地放开部分区间[0, m],m会动态变大缩小.因为Exchanger是要达成线程配对,所以尽量保持较小的开放区间,这样更容易找到配对的线程
private volatile int bound;
```

```text
bound 的位级结构


+----------------------------+-----------------+
|        序列号/代数           |   最大索引       |
+----------------------------+-----------------+       
31                         8 | 7              0 

低8位(b & MMASK) → 当前arena已放开的最大slot索引
高位(b & ~MMASK) → 版本号/代数(generation/sequence)

更新bound时,需要同步更新generation
比如已放开最大slot索引增加时,需要同步自增generation, 操作为 U.compareAndSwapInt(this, BOUND, b, b + SEQ + 1)  其中(+ SEQ)是为了自增generation, (+ 1)是为了增加已放开slot索引最大值.
当冲突发生时,通过扩容来降低并发竞争.
```



```text
static final class Node {
    int index;              // 记录线程循环arena槽位时的索引值
    int bound;              // Last recorded value of Exchanger.bound
    int collides;           // 记录bound的某个generation下发生的冲突次数(即cas失败)
    int hash;               // 伪随机数,用于将自旋周期随机化,避免自旋周期同频
    
    Object item;            // This thread's current item
    volatile Object match;  // Item provided by releasing thread
    volatile Thread parked; // Set to this thread when parked, else null
}
```

```text
// arena中的元素在放置时是以固定的步长(1<<7)来安排的,常见的CPU Cache-Line为64字节,这样arena的2个元素的地址间距就是2^7*1B=128字节,即每个元素独占2个cache-line,从而可以避免伪共享
private static final int ASHIFT = 7;

// 为了arena的工作集必须能被L1 cache覆盖,arena不能无限扩,需要为slot数设置理论上限. 32KB正好是L1 cache量级,256 slots≈32KB
// MMASK为理论最大slot索引值
private static final int MMASK = 0xff;

// 基于机器并行度(即同一时刻真正运行的线程数≈CPU核数),避免无意义的过度分槽
// 机器并行度足够高时,arena允许扩到设计极限MMASK,并行度没有超过slot设计极限时,slot index上限是NCPU/2 (除以二是因为Exchanger的slot是成对交换的)
static final int FULL = (NCPU >= (MMASK << 1)) ? MMASK : NCPU >>> 1;


bound & MMASK → 当前允许的最大slot索引值
FULL → slot索引值的理论上限
```

### exchange

```text
public V exchange(V x) throws InterruptedException {
    Object v;
    Object item = (x == null) ? NULL_ITEM : x; // translate null args
    if ((arena != null ||
         (v = slotExchange(item, false, 0L)) == null) &&
        ((Thread.interrupted() || // disambiguates null return
          (v = arenaExchange(item, false, 0L)) == null)))
        throw new InterruptedException();
    return (v == NULL_ITEM) ? null : (V)v;
}

等价体
public V exchange(V x) throws InterruptedException {
    Object v;
    Object item = (x == null) ? NULL_ITEM : x; // translate null args
    
    // 如果arena为null,大概率没有发生2个以上并发线程,优先用开销更小的slot来交换           (实际是否发生了大于2个以上并发线程,还要看slotExchange的返回值) 
    if (arena == null && (v = slotExchange(item, false, 0L)) != null) {
        return (v == NULL_ITEM) ? null : (V)v;
    }
    
    // arena!=null 或者 slotExchange返回null
    
    // slotExchange返回null的话,要么是因为当前线程发生了中断,要么是slot交换不适用需要使用arena来交换,所以在使用arenaExchange前需要先判断当前线程是否发生了中断
    if (!Thread.interrupted() && (v = arenaExchange(item, false, 0L)) != null) {
        return (v == NULL_ITEM) ? null : (V)v;
    }
    
    throw new InterruptedException();
}
```

### slotExchange

Returns: 
        the other thread's item; 
        or null if either the arena was enabled or the thread was interrupted before completion;
        or TIMED_OUT if timed and timed out

slotExchange是arenaExchange的低阶版本,slotExchange适用于一对线程,arenaExchange适用于多对线程.

spin->yield->block/cancel

```text
private final Object slotExchange(Object item, boolean timed, long ns) {
    Node p = participant.get();
    Thread t = Thread.currentThread();
    if (t.isInterrupted()) // preserve interrupt status so caller can recheck
        return null;

    for (Node q;;) {
        if ((q = slot) != null) {                                                   // ②后到的线程发现slot已经有对方的node了
            if (U.compareAndSwapObject(this, SLOT, q, null)) {                            // ②然后将对方node从slot移除,并将对方node中的item取走,并将自己的item放到对方node的match字段
                Object v = q.item;
                q.match = item;
                Thread w = q.parked;
                if (w != null)                                                                // ② slot被置空且看到parked设置后,unpark唤醒对方线程 (💯这是并发线性化点)
                    U.unpark(w);
                
                return v;
            }
            
            // 看到slot已经有值,现在cas居然失败了,很有可能被竞争者抢先一步取走了, 此时意味着并发度比较高,触发arena初始化,需要升级到arenaExchange方法
            // create arena on contention, but continue until slot null
            if (NCPU > 1 && bound == 0 && U.compareAndSwapInt(this, BOUND, 0, SEQ))
                arena = new Node[(FULL + 2) << ASHIFT];
        }
        else if (arena != null) // 💯slot==null&&arena!=null,需要升级到arenaExchange方法中去完成
            return null;
        else {                  // slot==null&&arena==null                          // ①先到达的线程一个循环到这里,将自己的item放到自己的node中,然后将自己的node放入slot中
            p.item = item;
            if (U.compareAndSwapObject(this, SLOT, null, p))
                break;
            p.item = null;
        }
    }

    // await release
    int h = p.hash;
    long end = timed ? System.nanoTime() + ns : 0L;
    int spins = (NCPU > 1) ? SPINS : 1;
    Object v;
    while ((v = p.match) == null) {                                // 只要对方还未放item,就可以一直循环
        // 先自旋等待一会
        if (spins > 0) {
            h ^= h << 1; h ^= h >>> 3; h ^= h << 10;
            if (h == 0)
                h = SPINS | (int)t.getId();
            else if (h < 0 && (--spins & ((SPINS >>> 1) - 1)) == 0)
                Thread.yield();
        }
        else if (slot != p)
            spins = SPINS;   // 临界点再为自旋续命一个周期
        
        // 后面都是自旋结束后发生的
        
        else if (!t.isInterrupted() && arena == null && (!timed || (ns = end - System.nanoTime()) > 0L)) {   // 未超时或中断
            U.putObject(t, BLOCKER, this);
            
            // 先设置parked且判断slot没有被置空,才能挂起等待           | 本方的parked/slot和对方的slot/parked的变量操作可以构成排他性,防止遗漏唤醒而睡死    (💯这是并发线性化点)
            p.parked = t;
            if (slot == p)
                U.park(false, ns);
            
            p.parked = null;
            U.putObject(t, BLOCKER, null);
        }
        else if (U.compareAndSwapObject(this, SLOT, p, null)) {                         // 中断/超时/arena不为null 后才能到这个elseif               (但是当arena!=null时,slot的值已经变为null,当前的cas是会失败的,然后回到while的判断表达式,此时p.match已经非null,循环结束)
            v = timed && ns <= 0L && !t.isInterrupted() ? TIMED_OUT : null;                     // cas成功表示releaser还未到来,返回特殊值TIMED_OUT或者null    (如果走到最后一个while循环的话,此时返回null只有可能是当前线程发生了中断💯)
            break;
        }
    }
    U.putOrderedObject(p, MATCH, null);
    p.item = null;
    p.hash = h;
    
    return v;
}
```

```text
slotExchange的简化模型

for (;;) {
  if (slot is empty) {                       // offer
    place item in a Node;
    if (can CAS slot from empty to node) {
      wait for release;
      return matching item in node;
    }
  }
  else if (can CAS slot from node to empty) { // release
    get the item in node;
    set matching item in node;
    release waiting thread;
  }
  // else retry on CAS failure
}

使用单个slot字段在并发线程少的时候是没问题的,当并发线程多的时候,性能会严重下降.
This works great in principle. 
But in practice, like many algorithms centered on atomic updates to a single location, it scales horribly when there are more than a few participants using the same Exchanger.
```

### arenaExchange

arena数组已被初始化,此方法中的一些处理方式和slotExchange比较类似,它是通过遍历arena数组找到需要交换的数据.

spin->yield->block/cancel

```text
private final Object arenaExchange(Object item, boolean timed, long ns) {
    Node[] a = arena;
    Node p = participant.get();
    
    // 无限循环,直到交换成功/超时/中断
    for (int i = p.index;;) {                                                               // 每个线程都从槽位arena[0]开始尝试
        int b, m, c; long j;
        Node q = (Node)U.getObjectVolatile(a, j = (i << ASHIFT) + ABASE);                   // 槽位arena[i]的地址, arena中的元素在放置时是以固定的步长(1<<7)来安排的,这样可以避免伪共享
        
        
        // 💯槽位arena[j]非空,说明槽位arena[j]有等待者,CAS将槽位清空,成为arena[j]等待者的releaser
        if (q != null && U.compareAndSwapObject(a, j, q, null)) {
            Object v = q.item;                     // release
            q.match = item;
            Thread w = q.parked;
            if (w != null)
                U.unpark(w);
            return v;
        }
        

        // 💯槽位arena[j]为空,尝试成为槽位arena[j]的等待者
        else if (i <= (m = (b = bound) & MMASK) && q == null) {                       // 遍历变量i<=m(当前允许的最大slot索引值)
            p.item = item;
            // 💯把当前线程的节点p放入到槽位arena[j]
            if (U.compareAndSwapObject(a, j, null, p)) {
                long end = (timed && m == 0) ? System.nanoTime() + ns : 0L;          // m==0意味着arena低并发,才可能需要挂起等待
                Thread t = Thread.currentThread();
                
                // 当前线程的节点p放入到槽位arena[j]后,等待releaser到来(先自旋等待,再挂起等待)
                for (int h = p.hash, spins = SPINS;;) {                               // h = p.hash作为伪随机数,初始值是0,伪随机数用于自旋周期随机化防止同频
                    Object v = p.match;
                    // 如果releaser已经取走当前线程item后且已经在.match设置好releaser的item
                    if (v != null) {
                        U.putOrderedObject(p, MATCH, null);
                        p.item = null;             // clear for next use
                        p.hash = h;
                        return v;
                    }
                    
                    
                    // 自旋等待
                    else if (spins > 0) {
                        h ^= h << 1; h ^= h >>> 3; h ^= h << 10; // xorshift
                        if (h == 0)
                            h = SPINS | (int)t.getId();                               // 使用xorshift生成伪随机数时,要防止退化到0.如果发生退化到0之后,xorshift就失效了.
                        else if (h < 0 &&  (--spins & ((SPINS >>> 1) - 1)) == 0)      // approx 50% true
                            Thread.yield();        // two yields per wait
                    }
                    else if (U.getObjectVolatile(a, j) != p)
                        spins = SPINS;                                                // releaser已经取走槽位,还未设置match字段的操作临界点,不必过早挂起,再自旋一会
                    
                    
                    // 未发生中断/超时,且arena其他槽位是空的(m>0时,不要挂起可以去尝试其他arena槽位)
                    else if (!t.isInterrupted() && m == 0 && (!timed || (ns = end - System.nanoTime()) > 0L)) {
                        U.putObject(t, BLOCKER, this);                                             // 模拟LockSupport.park
                        p.parked = t;             
                        if (U.getObjectVolatile(a, j) == p)                                       // minimize window
                            U.park(false, ns);
                        p.parked = null;
                        U.putObject(t, BLOCKER, null);
                    }
                    
                    // 如果发生了中断/超时,或者(m>0, 自旋后还没有配对成功),将当前线程的节点从槽位arena[j]撤销
                    else if (U.getObjectVolatile(a, j) == p && U.compareAndSwapObject(a, j, p, null)) {
                        if (m != 0)
                            U.compareAndSwapInt(this, BOUND, b, b + SEQ - 1);   // 并尝试缩容
                        
                        p.item = null;
                        p.hash = h;
                        i = p.index >>>= 1;        // 遍历索引减半,重新遍历
                        if (Thread.interrupted())
                            return null;
                        if (timed && m == 0 && ns <= 0L)
                            return TIMED_OUT;
                        break;                     // expired; restart
                    }
                }
            }
            else
                p.item = null;                     // 当前线程的节点p放入到槽位arena[j]失败就重试,清空之前设置的p.item
        }
        
        // 💯没有成为槽位arena[j]的等待者/releaser,即在槽位arena[j]发生了竞争,尝试下个槽位
        else {
            if (p.bound != b) {                    // bound变化,重置冲突计数(即cas失败次数)
                p.bound = b;
                p.collides = 0;
                i = (i != m || m == 0) ? m : m - 1;    // 等价转换 i = (i==m && m!=0) ? m-1: m   bound的generation发生了变化,下次遍历错开, 一个从m-1,其他从m
            }
            // (前提:p.bound == b bound未变化) 当前代内,在不扩容前,继续在现有arena槽位中寻找交换机会
            else if ((c = p.collides) < m || m == FULL ||  !U.compareAndSwapInt(this, BOUND, b, b + SEQ + 1)) {
                p.collides = c + 1;                // 冲突计数器++                                 
                i = (i == 0) ? m : i - 1;          // 倒序的环形遍历 (从当前槽位向“左”移动,到0后回绕到m)      | 老线程(非扩容线程)从高位向低位扩散
            }
            else
                i = m + 1;                         // 当前代内,扩容成功后,从当前槽位向“右”移动               | 扩容线程优先占用高位slot
            p.index = i;
        }
    }
}

else if ((c = p.collides) < m || m == FULL ||  !U.compareAndSwapInt(this, BOUND, b, b + SEQ + 1)) 中想要执行cas必须满足(c = p.collides)>=m && m != FULL,这样安排就是为了,
(c = p.collides)<m  当前代内,在将所有允许的slot尝试完前,不值得扩容(即扩大可用的slot区间,并非真的对arena数组扩容)
m == FULL           当扩容达到理论值,不再扩容

扩容发生在第一个releaser在U.compareAndSwapObject(a, j, q, null)成功后unpark第一个waiter前,又来了一个参与者成为了第二个waiter,此时发生了扩容.
```


### 

```text

```