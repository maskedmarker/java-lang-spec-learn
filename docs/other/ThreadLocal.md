# ThreadLocal

```text
WeakReference的referent字段在GC标记阶段是特殊处理的：不会沿着weak reference继续标记
```

```text
在jdk中,

thread ➡ threadLocalMap ➡ entry ==> threadLocal (被弱引用)
                            │
                            └➡ value (被强引用)
                            

在jdk中,value是被强引用, threadLocal是被弱引用.
当用户代码不再强引用threadLocal时,threadLocal会被gc回收,此时value值会被jdk当作无用数据,会被新的(threadLocal, value)占用原有的entry,覆盖旧的value值.
线程池中的线程通常与应用的生命周期一样长,如果用户不主动释放value值,value值会被thread一直强引用,导致内存泄露.(threadLocal会被gc回收,不会内存泄露).
```

```text
ThreadLocalMap.table被设计为只会扩容不会缩容.

为什么 JDK 不做缩容?
这是一个典型的设计权衡(trade-off): ThreadLocal设计初衷就是为高性能准备的,假设了每线程少量ThreadLocal变量;默认假设不会频繁增删,从而避免缩容的rehash导致性能抖动.
```


```text
java.lang.ThreadLocal.get
    java.lang.ThreadLocal.getMap
        java.lang.ThreadLocal.ThreadLocalMap.getEntry
        

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

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;                         // threadLocals时线程对象的属性
}

private Entry getEntry(ThreadLocal<?> key) {
    int i = key.threadLocalHashCode & (table.length - 1);
    Entry e = table[i];
    if (e != null && e.get() == key)              // 防止entry弱引用的threadLocal被gc掉
        return e;
    else
        return getEntryAfterMiss(key, i, e);
}
```

```text
static class Entry extends WeakReference<ThreadLocal<?>> {
    Object value;

    Entry(ThreadLocal<?> k, Object v) {
        super(k);
        value = v;
    }
}


static class ThreadLocalMap {
    
    private Entry[] table;     // 这里没有使用hash桶,如果发生hash碰撞的话,就去找后面可用的slot,如果slot不够用就扩容. 且table只会扩容不会缩容.🎯🎯🎯
    
    private static int nextIndex(int i, int len) {
        return ((i + 1 < len) ? i + 1 : 0);
    }
    
    private void set(ThreadLocal<?> key, Object value) {

        // We don't use a fast path as with get() because it is at least as common to use set() to create new entries as
        // it is to replace existing ones, in which case, a fast path would fail more often than not.

        Entry[] tab = table;
        int len = tab.length;
        int i = key.threadLocalHashCode & (len-1);

        for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {   // 采用开放寻址法
            ThreadLocal<?> k = e.get();
            
            if (k == key) {
                e.value = value;
                return;
            }
            
            // 如果entry弱引用的threadLocal被gc掉,就可以复用entry对象🎯🎯🎯
            if (k == null) {
                replaceStaleEntry(key, value, i);
                return;
            }
        }

        tab[i] = new Entry(key, value);
        int sz = ++size;
        if (!cleanSomeSlots(i, sz) && sz >= threshold)
            rehash();
    }
    
    private void remove(ThreadLocal<?> key) {
        Entry[] tab = table;
        int len = tab.length;
        int i = key.threadLocalHashCode & (len-1);
        for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
            if (e.get() == key) {
                e.clear();                              // remove操作也仅仅是清除weakReference.referent,不会删除entry的🎯🎯🎯
                expungeStaleEntry(i);
                return;
            }
        }
    }
}
```

