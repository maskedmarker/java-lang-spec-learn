# juc-LongAdder

```text
以"空间换时间".

AtomicLong 是单点竞争，LongAdder 是分散竞争，适合写多读少的场景。

LongAdder 采用 “热点分离” 思想。它将一个单一的 value 值，扩展为一个数组（Cell 数组，即单元格数组）。当并发增加时，每个线程会映射到数组中的某一个 Cell 进行 CAS 操作。最后需要获取总值时，再将所有 Cell 的值与基础值累加返回。
AtomicLong 采用 CAS（Compare-And-Swap，比较并交换） 机制。在高并发下，大量线程会同时尝试修改同一个变量，导致大量线程自旋重试（CAS 失败），从而造成 CPU 资源的浪费和性能下降。
```

```text
二、适用场景

(高频更新，低频读取)

1. 高并发下的统计计数
2. 限流器或滑动窗口的数据收集
3. 并行计算中的累加器
4. 轻量级的性能指标监控
```


## Striped64

```text
abstract class Striped64 extends Number {

    // Number of CPUS, to place bound on table size
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    
    // Table of cells. When non-null, size is a power of 2.
    transient volatile Cell[] cells;
    
    // Base value, used mainly when there is no contention, but also as a fallback during table initialization races.
    transient volatile long base;
    
    // pinlock (locked via CAS) used when resizing and/or creating Cells.
    transient volatile int cellsBusy;    // 操作cells及其数组元素时的cas锁
}
```

```text
final void longAccumulate(long x, LongBinaryOperator fn, boolean wasUncontended) {
    int h;
    if ((h = getProbe()) == 0) {
        ThreadLocalRandom.current(); // force initialization
        h = getProbe();
        wasUncontended = true;
    }
    
    boolean collide = false;                // True if last slot nonempty
    for (;;) {
        Cell[] as; Cell a; int n; long v;
        if ((as = cells) != null && (n = as.length) > 0) {
            if ((a = as[(n - 1) & h]) == null) {                              // 用Thread.probe来定位当前线程的cell
                if (cellsBusy == 0) {       // Try to attach new Cell
                    Cell r = new Cell(x);   // Optimistically create
                    if (cellsBusy == 0 && casCellsBusy()) {
                        boolean created = false;
                        try {               // Recheck under lock
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        if (created)
                            break;
                        continue;           // Slot is now non-empty
                    }
                }
                collide = false;
            }
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            else if (a.cas(v = a.value, ((fn == null) ? v + x : fn.applyAsLong(v, x))))
                break;
            else if (n >= NCPU || cells != as)
                collide = false;            // At max size or stale
            else if (!collide)
                collide = true;
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    if (cells == as) {      // Expand table unless stale
                        Cell[] rs = new Cell[n << 1];                           // cells扩容
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }
            h = advanceProbe(h);
        }
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {                 // 争抢初始化cells
            boolean init = false;
            try {                           // Initialize table
                if (cells == as) {
                    Cell[] rs = new Cell[2];                                        // 初始化cells的长度为2
                    rs[h & 1] = new Cell(x);
                    cells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            if (init)
                break;
        }
        else if (casBase(v = base, ((fn == null) ? v + x : fn.applyAsLong(v, x))))     // 初始化竞争失败,累加到base上
            break;                          // Fall back on using base
    }    
}
```


## LongAdder

```text
public class LongAdder extends Striped64 implements Serializable {


    public void add(long x) {
        Cell[] as; long b, v; int m; Cell a;
        if ((as = cells) != null || !casBase(b = base, b + x)) {    // 如果cell为空,且cas-base成功就结束了;如果cell不为空,就去cas-cell[i]
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value, v + x)))
                longAccumulate(x, null, uncontended);
        }
    }


    public long sum() {
        Cell[] as = cells; Cell a;
        long sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }
    
    public long sumThenReset() {
        Cell[] as = cells; Cell a;
        long sum = base;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    sum += a.value;
                    a.value = 0L;
                }
            }
        }
        return sum;
    }    
}
```
