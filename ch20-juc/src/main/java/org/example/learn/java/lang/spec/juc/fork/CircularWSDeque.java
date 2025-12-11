package org.example.learn.java.lang.spec.juc.fork;

/**
 *
 * <<Dynamic Circular Work-Stealing Deque>> 论文中优化了常用于调度器的无锁调度算法. 该算法也被fork-join使用.
 * 如下代码是一个简化版,不涉及数组的缩容
 *
 * The deque’s owner process pushes and pops local work to and from the deque’s bottom end.
 * To minimize synchronization overhead for the deque’s owner, stolen elements are taken from the top end of the deque.
 * Note that pushBottom and popBottom operations are invoked only by the deque’s owner.
 *
 * 循环不变式 top<=bottom
 *
 * 备注: ForkJoinPool.WorkQueue使用了该算法的变体版本
 */
public class CircularWSDeque {

    public final static Object Empty = new Object();
    public final static Object Abort = new Object();
    private final static int LogInitialSize = 3;

    private volatile CircularArray activeArray = new CircularArray(LogInitialSize);
    private volatile int bottom = 0;  // 待插入元素的索引(还未添加)
    private volatile int top = 0;     // 待steal元素的索引(已经添加)(precondition: bottom>top)  区间[top, bottom)持有元素;[bottom, size-1],[0, top)区间是空的  (注意这里用的是环形数组)

    public void pushBottom(Object o) {
        int b = this.bottom;
        int t = this.top;
        CircularArray a = this.activeArray;
        int size = b - t;
        if (size >= a.size() - 1) { // 为了缩容,不多解释
            a = a.grow(b, t);
            this.activeArray = a;
        }

        a.put(b, o);
        this.bottom = b + 1;  // 📌pushBottom操作的线性化点: bottom加一
    }

    /**
     * steal操作取出的是(top)的元素.
     */
    public Object steal() {
        int t = this.top;
        int b = this.bottom;
        CircularArray a = this.activeArray;
        int size = b - t;
        if (size <= 0) {  // 队列无数据,steal返回空   || 保证循环不变式 top<=bottom
            return Empty;
        }
        // bottom>top:当队列有数据时
        Object o = a.get(t); // 类似于peek并非移除
        if (!casTop(t, t + 1)) {  // cas-top防止并发的steal/popBottom     // 📌steal操作的线性化点: top加一
            return Abort;
        }
        return o;
    }

    /**
     * pushBottom/popBottom只能是owner线程操作.
     * 当owner线程执行popBottom时,没有线程会执行pushBottom,而steal操作不涉及bottom.所以bottom是线程安全的.
     *
     * popBottom操作取出的是(bottom-1)的元素. 当bottom=top+1时,popBottom操作和steal操作会去操作同一个元素
     */
    public Object popBottom() {
        int b = this.bottom;
        CircularArray a = this.activeArray;
        b = b - 1;
        this.bottom = b;
        int t = this.top;

        int size = b - t;
        if (size < 0) {      // bottom>=top而此时bottom<top+1,即top+1>bottom>=top那么可以得出初始态bottom==top,也就是初始态队列无数据
            this.bottom = t;
            return Empty;
        }

        Object o = a.get(b);
        if (size > 0){       // bottom>=top而此时bottom>top+1,即bottom>top+1,也就是初始态队列数据量大于1
            return o;        // 📌popBottom操作的线性化点: 保证bottom>top+1下bottom减一
        }

        // 此时size只能是等于0. 即bottom==(top+1),队列中只剩余1个元素,此时popBottom/steal并发时会操作同一个元素
        if (!casTop(t, t + 1)) {
            o = Empty;
        }
        this.bottom = t + 1;  // 📌popBottom操作的线性化点: 保证bottom==(top+1)下top加一,bottom加一           || 保证循环不变式 top<=bottom
        return o;
    }


    private boolean casTop(long oldVal, long newVal) {
        return U.compareAndSwapLong(this, TOP, oldVal, newVal);
    }


    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long TOP;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = CircularWSDeque.class;
            TOP = U.objectFieldOffset(k.getDeclaredField("top"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    // a dynamic-cyclic-array
    static class CircularArray {
        // 定义数组长度的指数
        private int logSize;
        // 数组本体
        private Object[] segment;

        CircularArray(int logSize) {
            this.logSize = logSize;
            this.segment = new Object[1 << this.logSize];
        }

        int size() {
            return 1 << this.logSize;
        }

        Object get(int i) {
            return this.segment[i % size()];
        }

        void put(int i, Object o) {
            this.segment[i % size()] = o;
        }

        CircularArray grow(int b, int t) {
            CircularArray a = new CircularArray(this.logSize + 1); // 扩容1倍
            for (int i = t; i < b; i++) {
                a.put(i, this.get(i)); // 将原来数组的元素copy到新数组(保持原数组不变)
            }

            return a;
        }
    }
}
