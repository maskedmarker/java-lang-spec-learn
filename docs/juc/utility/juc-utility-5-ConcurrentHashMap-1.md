# juc-ConcurrentHashMap



## ConcurrentHashMap实例字段


```text
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {
    
    // The array of bins. Lazily initialized upon first insertion. Size is always a power of two. Accessed directly by iterators.
    transient volatile Node<K,V>[] table;

    // The next table to use; non-null only while resizing.💯💯💯
    private transient volatile Node<K,V>[] nextTable;
    
    // Table initialization and resizing control. 
    // When negative, the table is being initialized or resized: -1 for initialization, else -(1 + the number of active resizing threads). 
    // Otherwise, when table is null, holds the initial table size to use upon creation, or 0 for default.  After initialization, holds the next element count value upon which to resize the table.
    private transient volatile int sizeCtl;
    
    
    // The next table index (plus one) to split while resizing.
    private transient volatile int transferIndex;
}
```

## 构造函数

```text
public ConcurrentHashMap(int initialCapacity) {
    if (initialCapacity < 0) throw new IllegalArgumentException();
    
    // 用户期望的容量大于等于理论大容量的一半时,直接使用理论最大容量
    // initialCapacity+(initialCapacity >>> 1)=initialCapacity*1.5是作为整数位移+加法的高效近似实现,最接近initialCapacity*(1/0.75F)
    // +1 是一个精心设计的边界修正项,用来抵消整数位移带来的系统性向下偏差,并避免 ConcurrentHashMap在初始化后立即落入“扩容临界点”
    int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
    this.sizeCtl = cap;
}
```

## 常量

```text
    // The largest possible table capacity. 
    // This value must be exactly 1<<30 to stay within Java array allocation and indexing bounds for power of two table sizes, 
    // and is further required because the top two bits of 32bit hash fields are used for control purposes.💯
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    
    // The load factor for this table. 
    // Overrides of this value in constructors affect only the initial table capacity. 
    // The actual floating point value isn't normally used -- it is simpler to use expressions such as n - (n >>> 2) for the associated resizing threshold. 0.75f==(1-1/4)
    private static final float LOAD_FACTOR = 0.75f;
    
    
    // The bin count threshold for using a tree rather than list for a bin. 
    static final int TREEIFY_THRESHOLD = 8;
    // The bin count threshold for untreeifying a (split) bin during a resize operation. 
    static final int UNTREEIFY_THRESHOLD = 6;
    
    
    // The smallest table capacity for which bins may be treeified. (Otherwise the table is resized if too many nodes in a bin.) The value should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts between resizing and treeification thresholds.
    static final int MIN_TREEIFY_CAPACITY = 64;
    
    
    // The number of bits used for generation stamp in sizeCtl. Must be at least 6 for 32bit arrays. (sizeCtl的高)
    private static int RESIZE_STAMP_BITS = 16;
    
    
    // The maximum number of threads that can help resize. Must fit in 32 - RESIZE_STAMP_BITS bits.
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
    
    
    // The bit shift for recording size stamp in sizeCtl.
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;
```

## sizeCtl

```text
在非扩容状态：
sizeCtl == -1  ⇒  初始化中
sizeCtl > 0    ⇒  扩容的阈值(通常为数组的大小的0.75)

在扩容期间：
sizeCtl = (resizeStamp << RESIZE_STAMP_SHIFT) + helpers
其中：
sizeCtl < 0 ⇒ 正在扩容,在扩容期间保持(sizeCtl < 0)直到扩容结束
helpers = 参与扩容的线程数
其位布局(32 bit int): [ resizeStamp | helpers ]即 sizeCtl = (resizeStamp(n) << RESIZE_STAMP_SHIFT) + helpers
```