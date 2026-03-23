package org.example.learn.java.lang.spec.other.mis;

import java.util.concurrent.atomic.AtomicLong;

/**

 WeakReference 的 referent 字段是特殊处理的 GC 标记阶段： 不会沿着 weak reference 继续标记

 thread -> threadLocalMap -> entry ==> threadLocal
                                |
                                -> value

 在jdk中,value是被强引用, threadLocal是被弱引用.
 当用户代码不再强引用threadLocal时,threadLocal会被gc回收,此时value值会被jdk当作无用数据,会被新的(threadLocal, value)占用原有的entry,覆盖旧的value值.

 线程池中的线程通常与应用的生命周期一样长,如果用户不主动释放value值,value值会被thread一直强引用,导致内存泄露.(threadLocal会被gc回收,不会内存泄露)
 */
public class ThreadLocalTest {

    private static AtomicLong SEQ_GENERATOR = new AtomicLong(1000);

    // 当线程存在时,Thread对象强引用ThreadLocalMap对象,ThreadLocalMap对象强引用ThreadLocalMap.Entry,ThreadLocalMap.Entry对象弱引用ThreadLocal对象
    // 用户对象ThreadLocalTest强引用ThreadLocal对象
    // 只要用户对象还存在,那么ThreadLocal对象就不会回收.如果用户对象会回收,ThreadLocal对象就是弱引用,会被gc回收(因为此时没有用户代码可以引用了,ThreadLocal对象被回收也是合情合理的)
    // (threadLocal,value)对中threadLocal不被业务代码引用时会被gc回收,但是value值是强引用.在线程池环境下,线程只要不销毁,那么value值已知得不到释放,会造成内存泄露.
    // 所以需要用户在不使用value值后,需要手动调用threadLocal.remove()来结束ThreadLocalMap.Entry对value的强引用
    private static final ThreadLocal<Long> BIZ_KEY = ThreadLocal.withInitial(() -> SEQ_GENERATOR.getAndIncrement());

    public void test0() {
        Long seq = BIZ_KEY.get();
    }
}
