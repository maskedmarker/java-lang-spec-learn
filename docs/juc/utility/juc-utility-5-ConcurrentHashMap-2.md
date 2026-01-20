# juc-ConcurrentHashMap


## 核心方法

### putVal

```text
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());                                                        // 将hashCode的高位信息混入低位,同时强制结果为非负值
    
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)                                             // 首次插入数据时,触发初始化过程
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {                              // ((n - 1)&hash)基于哈希值计算桶i,如果桶i没被占用  (注意这里使用的术语是哈希桶bin,而非槽位slot)
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;                                                                        // (最理想的场景) 通过cas将value设置到哈希桶table[i]
        }
        
        else if ((fh = f.hash) == MOVED)                                                     // 通过spread计算,正常Node.hash值都是非负数, MOVED为-1,是特殊值
            tab = helpTransfer(tab, f);                                                      // 协助扩容
        
        else {
            V oldVal = null;
            synchronized (f) {                                                                // 对某个哈希桶加锁💯
                if (tabAt(tab, i) == f) {
                    // 哈希桶还未树化
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {                                  // binCount用来记录哈希桶的链表长度
                            K ek;
                            // 如果哈希桶的链表头节点的hash值相等且equals为true,替换头节点的旧value
                            if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            
                            // 否则在哈希桶的链表的尾部新增节点
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key, value, null);
                                break;
                            }
                        }
                    }
                    
                    // 哈希桶已经树化
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key, value)) != null) {         // 往树中插入节点
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            
            // 链表长度(binCount)过长时,就要将哈希桶的链表转换为树型数据结构
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);                                                   // 对某个哈希桶加锁💯
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

```text
static final int spread(int h) {
    // (n - 1) & hash在计算哈希桶索引时,只涉及到低log2(n)位,将hashCode的高低位混淆后,增加低索引的离散度
    // & HASH_BITS 强制结果为非负值
    return (h ^ (h >>> 16)) & HASH_BITS;
}
```

### initTable

将sizeCtl的-1值作为特殊值,充当初始化table的cas锁标识

```text
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)
            Thread.yield(); // lost initialization race; just spin
        
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {          // 先将sizeCtl设置为-1,作为抢占初始化权力成功标识
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;           // sizeCtl可以在构造函数中设定初始容器大小,如果用户没有设置就使用默认值
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);                                 // 1-1/4=3/4=0.75
                }
            } finally {
                sizeCtl = sc;                                           // 抢占初始化权力成功后,设置sizeCtl的大小  (table是判断的关键变量,sizeCtl可以后设置)
            }
            break;
        }
    }
    return tab;
}
```

### replaceNode

Implementation for the four public remove/replace methods: Replaces node value with v, conditional upon match of cv if non-null. If resulting value is null, delete.

执行替换操作replace-value:
1.入参value不为null且入参cv(conditional-value)与节点的e.value相等
2.入参value不为null且入参cv(conditional-value)为null (cv==null类似于通配符)
执行移除操作remove-node:
1.入参value为null且入参cv与节点的e.value相等
1.入参value为null且入参cv为null (cv==null类似于通配符)

```text
final V replaceNode(Object key, V value, Object cv) {
    int hash = spread(key.hashCode());
    
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0 || (f = tabAt(tab, i = (n - 1) & hash)) == null)
            break;
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        
        else {
            V oldVal = null;
            boolean validated = false;
            synchronized (f) {                                                                                                  // 对哈希桶加锁
                if (tabAt(tab, i) == f) {                                                                                       // 同步常用的double-check
                    // 哈希桶还是链表未树化
                    if (fh >= 0) {
                        validated = true;
                        for (Node<K,V> e = f, pred = null;;) {
                            K ek;
                            if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {                    // hashCode/key相等时(即==或equals返回true)
                                V ev = e.val;
                                if (cv == null || cv == ev || (ev != null && cv.equals(ev))) {                                  // 支持remove/replace2种模式
                                    oldVal = ev;
                                    if (value != null)                                                                              // 如果cv==null或者cv与e.value相等(==或equals)且入参value不为null,则执行replace-value操作
                                        e.val = value;
                                    
                                    else if (pred != null)                                                                          // 如果cv==null或者cv与e.value相等(==或equals)且入参value为null,则执行remove-node操作
                                        pred.next = e.next;
                                    else
                                        setTabAt(tab, i, e.next);   // 哈希桶的链表头节点
                                }
                                break;
                            }
                            pred = e;
                            if ((e = e.next) == null)
                                break;
                        }
                    }
                    
                    // 哈希桶已经是红黑树(fh<0 && fh==-2)
                    else if (f instanceof TreeBin) {
                        validated = true;
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> r, p;
                        if ((r = t.root) != null && (p = r.findTreeNode(hash, key, null)) != null) {
                            V pv = p.val;
                            if (cv == null || cv == pv || (pv != null && cv.equals(pv))) {
                                oldVal = pv;
                                if (value != null)
                                    p.val = value;
                                else if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));                                                        // 基于TreeNode.first的链表视图,红黑树重新变为链表. 
                            }
                        }
                    }
                }
            }
            if (validated) {
                if (oldVal != null) {
                    if (value == null)
                        addCount(-1L, -1);
                    return oldVal;
                }
                break;
            }
        }
    }
    return null;
}
```


### get

在完全不加锁的前提下,安全地读取一个可能正在被并发修改的哈希桶.

get()无锁成立依赖以下事实:
Node一旦发布不可变key/hash
next指针是单向append-only语义
结构性变化用特殊节点(TreeBin/ForwardingNode)隔离
table和bin头节点是volatile可见的

```text
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    
    if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, (n - 1) & h)) != null) {            // e为哈希桶的头节点(普通Node/TreeNode/ForwardingNode)
        if ((eh = e.hash) == h) {                                                                            // h>0 eh==h>0 此时哈希桶还是链表,链表头直接命中
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        else if (eh < 0)                                                                                     // 此时哈希桶已经是红黑树或者正在扩容
            return (p = e.find(h, key)) != null ? p.val : null;                                                 // 💯💯注意:TreeNode.find/ForwardingNode.find
        
        // 链表头没有命中, 且TreeNode/ForwardingNode都没找到,接着继续在链表后面查找
        while ((e = e.next) != null) {
            if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    
    return null;
}
```




## 扩容


### resizeStamp

n==table.length
table.length是2的幂.
numberOfLeadingZeros(n)与log₂(n) 一一对应
因此：每一次扩容n翻倍;numberOfLeadingZeros(n)减1可作为容量世代标识.

|(1 << (RESIZE_STAMP_BITS - 1))强制resizeStamp的最高位为1
确保该值：在sizeCtl左移后,sizeCtl永远是负数,不与普通sizeCtl阈值冲突.

按位或(|)结果：
低15位：容量世代信息;第15位：扩容标志位

resizeStamp(int n)从sizeCtl中提取generation信息(并附带了高位1)
```text
static final int resizeStamp(int n) {   // n==table.length
    // Integer.numberOfLeadingZeros(table.length)作为当前generation
    // resizeStamp会被(<< RESIZE_STAMP_SHIFT)即(<< (32 - RESIZE_STAMP_BITS)),为了避免<<位移溢出,必须将generation的位长限制在(RESIZE_STAMP_BITS-1)内
    // 同时为了保证(resizeStamp<< (32 - RESIZE_STAMP_BITS))后最高位是1,需要在此时强制设置一个最高位是1, 意味着必须要求generation的位长限制在(RESIZE_STAMP_BITS-2)
    return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));           // | (1 << (RESIZE_STAMP_BITS - 1) 保证了resizeStamp最高位是1,generation的位长限制在(RESIZE_STAMP_BITS-2)
}
```

### tryPresize

Tries to presize table to accommodate the given number of elements.
Params: size – number of elements (doesn't need to be perfectly accurate)

```text
private final void tryPresize(int size) {
    int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(size + (size >>> 1) + 1);   // 达到最大理论容量一半时直接扩容到最大理论容量,否则1/0.75f(1.33≈1.5)扩容
    
    int sc;
    while ((sc = sizeCtl) >= 0) {
        Node<K,V>[] tab = table; int n;
        if (tab == null || (n = tab.length) == 0) {                                                         // tab初始化
            n = (sc > c) ? sc : c;
            if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if (table == tab) {
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
            }
        }
        
        else if (c <= sc || n >= MAXIMUM_CAPACITY)                                                         // 变量c就是目标容量,c <= sc表示此时容量已经达到目标容量
            break;
        else if (tab == table) {
            int rs = resizeStamp(n);
            
            // sizeCtl<0,此时正在扩容,当前线程试图去协助扩容
            if (sc < 0) {
                Node<K,V>[] nt;
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))                                         // 协助扩容前,先helper-count加一,协助完成后再helper-count减一
                    transfer(tab, nt);
            }
            
            // sizeCtl>0,当前线程是扩容发起者
            else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2))               // 发起扩容的线程,先helper-count加二(加二是为了判断谁是最后一个helper)
                transfer(tab, null);
        }
    }
}

每个参与扩容的线程,先helper加一,完成扩容工作后都会helper减一.
而首个参与者需要额外helper加一,这是留给helper减一后,判断当前线程是否是最后一个完成工作的helper,如果是的话,需要额外处理收尾工作(将table替换成nextTable值,然后nextTable重置为null)
```




```text
resizeStamp 如何参与并发扩容


1. 扩容开始时
int rs = resizeStamp(n);
sizeCtl = (rs << RESIZE_STAMP_SHIFT) + 2;

含义：
rs << shift：标识当前扩容世代;
+2：1个发起线程+保留位

2. 其他线程加入扩容
if ((sc >>> RESIZE_STAMP_SHIFT) == rs)   // 可以安全加入
核心校验点：只有stamp匹配当前table容量的线程,才能协助扩容


3. 防止 ABA / 跨代加入
如果：
table 已经完成一次扩容
n 已变化
resizeStamp(n) 不同

则：
旧线程 不会误加入新一轮扩容
```

### transfer

Moves and/or copies the nodes in each bin to new table. See above for explanation.

把旧table(tab)中的桶并发/安全地搬迁到新table(nextTab),并通过ForwardingNode标记已完成的桶,支持多线程协作扩容.

```text
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    int n = tab.length, stride;
    if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)                     // 计算每个线程的"搬迁步幅",即每个线程一次CAS领取的桶区间大小
        stride = MIN_TRANSFER_STRIDE;
    
    if (nextTab == null) {                                                                      // 初始化nextTable(只有一个线程会成功)
        try {
            @SuppressWarnings("unchecked")
            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];                                         // 容量翻倍
            nextTab = nt;
        } catch (Throwable ex) {                                                                        // 处理OOME: sizeCtl:=MAX_VALUE,直接禁止后续resize
            sizeCtl = Integer.MAX_VALUE;
            return;
        }
        nextTable = nextTab;
        transferIndex = n;
    }
    
    
    int nextn = nextTab.length;
    ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
    boolean advance = true;                                                                             // 变量advance:是否需要申请新区间
    boolean finishing = false; // to ensure sweep before committing nextTab                             // 变量finishing:标志“进入收尾阶段”+确保所有桶都被扫描过一次
    for (int i = 0, bound = 0;;) {                                                                      // i:当前处理的桶下标; bound:当前线程负责区间的下界(不含)
        Node<K,V> f; int fh;
        
        // 领取搬迁区间(无锁任务分配)
        while (advance) {
            int nextIndex, nextBound;
            if (--i >= bound || finishing)                                                              // 如果当前区间还有桶,继续在已经领取的区间扫描
                advance = false;
            else if ((nextIndex = transferIndex) <= 0) {                                                // transferIndex到达0时,tab[0]已经被分配走了,此时没有其他哈希桶需要处理了
                i = -1;                                                                                 // i = -1是为了强制走某一条控制流路径,这是一个控制流哨兵值
                advance = false;
            }
            else if (U.compareAndSwapInt(this, TRANSFERINDEX, nextIndex,nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {      // CAS领取一段[nextBound, nextIndex)区间的哈希桶     💯从区间[0,n]的高区间一块块分割领取
                bound = nextBound;
                i = nextIndex - 1;                                                                                                                  // i=nextIndex-1, --i:从高到低扫描[nextBound, nextIndex)区间的哈希桶
                advance = false;
            }
        }
        
        

        // if表达式的理解见后面解释
        if (i < 0 || i >= n || i + n >= nextn) {
            int sc;
            if (finishing) {                                                                            // 最后一个helper线程才会走到这里
                nextTable = null;                                                                                 // 扩容完成后重置nextTable
                table = nextTab;
                sizeCtl = (n << 1) - (n >>> 1);                                                                   // 设定扩容阈值为原数组容量的大约1/0.75≈1.5
                return;
            }
            
            if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {                                       // 当线程协助扩容时先helper-count加一,当完成了自己负责的区间,需要helper-count减一    
                if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)                                             // (n一直没有改动,sc来自刚读取的sizeCtl),如果当前线程不是最后一个完成transfer的helper,则在完成了自己负责的区间后helper-count减一后直接退出
                    return;
                finishing = advance = true;                                                                       // 当线程是“最后一个完成transfer的helper”,设置finishing:=true,进入提交新表table = nextTab
                i = n;                                                                                       // i = n是为了强制走某一条控制流路径,这是一个控制流哨兵值
            }
        }
        
        // 处理一个具体的哈希桶table[i]
        else if ((f = tabAt(tab, i)) == null)
            advance = casTabAt(tab, i, null, fwd);                                                    // 空桶直接标记为ForwardingNode,表示“已处理”
        else if ((fh = f.hash) == MOVED)
            advance = true;                                                                           // 已被其他线程处理,跳过当前哈希桶
        else {                                                                                        // 真正的数据迁移(需要加锁)
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    Node<K,V> ln, hn;
                    
                    // 哈希桶还是链表 
                    if (fh >= 0) {
                        // 同一个哈希桶中的元素的hashCode的0~log2(n)-1位的bit-sequence是相同的,这里&n是在计算hashCode的第log2(n)位是否相同,如果相同的话,扩容后仍在同一个哈希桶💯💯💯
                        // 
                        int runBit = fh & n;                                                                // n为2的幂,注意这里是&n,而非&(n-1)     | 正常计算哈希桶的索引时,通过将&(n-1)作为bit-mask出现的.
                        Node<K,V> lastRun = f;
                        for (Node<K,V> p = f.next; p != null; p = p.next) {                                 // 链表尾部从lastRun到末尾,是一段runBit相同的连续区间
                            int b = p.hash & n;                                                             // n为2的幂,注意这里是&n,而非&(n-1)
                            if (b != runBit) {
                                runBit = b;
                                lastRun = p;
                            }
                        }
                        
                        // ln代表扩容后的哈希桶索引值不变的链表尾部,hn代表扩容后的哈希桶索引值会变的链表尾部                                                                                    
                        if (runBit == 0) {                                                                   // 如果hashCode的第log2(n)位都是0,则区间[lastRun...尾元素]扩容后的哈希桶索引值不变
                            ln = lastRun;
                            hn = null;
                        }
                        else {                                                                               // 如果hashCode的第log2(n)位都是1,则区间[lastRun...尾元素]扩容后的哈希桶索引值会变为(i+n)
                            hn = lastRun;
                            ln = null;
                        }
                        
                        for (Node<K,V> p = f; p != lastRun; p = p.next) {                                   
                            int ph = p.hash; K pk = p.key; V pv = p.val;
                            if ((ph & n) == 0)
                                ln = new Node<K,V>(ph, pk, pv, ln);                                           // 从区间[头元素...lastRun)中挑选库容后哈希桶索引值不变的,拼接到ln  (此时会改变链表的元素顺序,但是Map中并没有保持顺序不变的语义要求,所以不所谓)
                            else
                                hn = new Node<K,V>(ph, pk, pv, hn);                                           // 从区间[头元素...lastRun)中挑选库容后哈希桶索引值会变的,拼接到hn  (此时会改变链表的元素顺序,但是Map中并没有保持顺序不变的语义要求,所以不所谓)
                        }
                        // 注意: 哈希桶的原链表并没有被改变,此时的读操作不受影响,最多是读到了旧链表数据
                        
                        // 原链表元素在扩容后的哈希桶索引只会是i或者(i+n)
                        setTabAt(nextTab, i, ln);
                        setTabAt(nextTab, i + n, hn);
                        //标记旧桶,在此之后的读操作就可以读到新数据.在原数组上设置一个重定向元素,将操作转移到新哈希桶上💯💯💯
                        setTabAt(tab, i, fwd);
                        advance = true;                                                                        // 这个哈希桶迁移完了
                    }
                    
                    // 哈希桶是红黑树
                    else if (f instanceof TreeBin) {
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> lo = null, loTail = null;
                        TreeNode<K,V> hi = null, hiTail = null;
                        int lc = 0, hc = 0;
                        for (Node<K,V> e = t.first; e != null; e = e.next) {                                // 遍历 TreeNode(按链表顺序)
                            int h = e.hash;
                            TreeNode<K,V> p = new TreeNode<K,V>
                                (h, e.key, e.val, null, null);
                            if ((h & n) == 0) {
                                if ((p.prev = loTail) == null)
                                    lo = p;
                                else
                                    loTail.next = p;
                                loTail = p;
                                ++lc;
                            }
                            else {
                                if ((p.prev = hiTail) == null)
                                    hi = p;
                                else
                                    hiTail.next = p;
                                hiTail = p;
                                ++hc;
                            }
                        }
                        ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                            (hc != 0) ? new TreeBin<K,V>(lo) : t;
                        hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                            (lc != 0) ? new TreeBin<K,V>(hi) : t;
                        setTabAt(nextTab, i, ln);
                        setTabAt(nextTab, i + n, hn);
                        setTabAt(tab, i, fwd);
                        advance = true;
                    }
                }
            }
        }
    }
}
```

```text
计算runBit的目的
runBit 的唯一目的：在扩容时,找出链表尾部“去向一致的一段节点”,这段节点可以直接复用.next的关系,不用重新复制新的Node对象.(扩容时不能修改原链表,只能替换)

resize时节点“去向”的基本规则(前提)
扩容时：
旧表容量 = n
新表容量 = 2n
对于链表中的任一节点 p：
    (p.hash & n) == 0   → 还在 index i
    (p.hash & n) != 0   → 去 index i + n
只取决于hash的第log2(n)位.

链表尾部,可能存在一段连续节点,它们的 (hash & n) 结果是相同的.

为什么只关心“最后一段”
因为：尾部这段可以直接挂到新表,不需要复制,不需要反转,不影响顺序;而前半段去向可能混杂,必须逐个处理.


用一个完整例子
假设：
旧链表：
A → B → C → D → E → F → null
(hash & n)：
A:0 B:1 C:0 D:1 E:1 F:1

扫描过程：
节点	 b	 runBit	 lastRun
A	 0	 0	     A
B	 1	 1	     B
C	 0	 0	     C
D	 1	 1	     D
E	 1	 1	     D
F	 1	 1	     D
最终：
lastRun = D
尾部 D → E → F 全部 hash & n == 1

结果：
hn = D → E → F(原节点复用)
ln 只由 A, B, C 复制构成
```

```text
解释 if (i < 0 || i >= n || i + n >= nextn) 

i是当前线程正在处理的bin-index. 
其值来源有三种：
    从transferIndex CAS抢到的一段区间;
    finishing阶段强制设置为n;
    无活可干时被置为-1

i < 0  是针对 无活可干时被置为-1    
i >= n 是针对 finishing阶段强制设置为n
i + n >= nextn 是一个防御式边界检查
```