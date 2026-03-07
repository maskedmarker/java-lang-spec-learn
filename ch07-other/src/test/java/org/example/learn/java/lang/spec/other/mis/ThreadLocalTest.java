package org.example.learn.java.lang.spec.other.mis;

import java.util.concurrent.atomic.AtomicLong;

/**

<pre>
java.lang.ThreadLocal#get()
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }

 java.lang.ThreadLocal#getMap()
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;    // Thread对象对ThreadLocalMap对象强引用
    }

class ThreadLocalMap {

    static class Entry extends WeakReference<ThreadLocal<?>> {
        // The value associated with this ThreadLocal.
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            // ThreadLocalMap.Entry对象对ThreadLocal对象是弱引用,对value是强引用
            super(k);
            value = v;
        }
    }

    private Entry[] table;

    private void set(ThreadLocal<?> key, Object value) {
        Entry[] tab = table;
        int len = tab.length;
        int i = key.threadLocalHashCode & (len-1);
        // ...
        tab[i] = new Entry(key, value);
        // ...
    }
}

</pre>


 */
public class ThreadLocalTest {

    private static AtomicLong SEQ_GENERATOR = new AtomicLong(1000);

    // 当线程存在时,Thread对象强引用ThreadLocalMap对象,ThreadLocalMap对象强引用ThreadLocalMap.Entry,ThreadLocalMap.Entry对象弱引用ThreadLocal对象
    // 用户对象ThreadLocalTest强引用ThreadLocal对象
    // 只要用户对象还存在,那么ThreadLocal对象就不会回收.如果用户对象会回收,ThreadLocal对象就是弱引用,会被gc回收(因为此时没有用户代码可以引用了,ThreadLocal对象被回收也是合情合理的)
    private static final ThreadLocal<Long> BIZ_KEY = ThreadLocal.withInitial(() -> SEQ_GENERATOR.getAndIncrement());

    public void test0() {
        Long seq = BIZ_KEY.get();
    }
}
