package org.example.learn.java.lang.spec.juc.fork;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class SimpleWorkQueue<T> {

    AtomicInteger base;         // index of next slot for poll (base is the index of the oldest element) (stealer线程操作)
    AtomicInteger top;                   // index of next slot for push (owner线程操作)

    ForkJoinTask<?>[] array;   // the elements (initially unallocated) (容纳任务, 使用方式是环形数组)


    final void push(ForkJoinTask<?> task) {
        // m-> mask
        int m = array.length - 1;
        // 在top处插入任务,然后top++
        // 检查是否需要扩容 (当快要满时,扩容)
    }

    final ForkJoinTask<?> pop() {
        return null;
        // 读取top-1处的任务t,
        // 然后通过cas设置null来实现原子操作   cas(array, top-1, t, null)
    }

    final ForkJoinTask<?> poll() {
        return null;
        // ①在base<top前提下,
            // ②读取base处的任务t,
                // ③如果t!=null然后通过cas设置null来实现原子操作
                // ④如果t==null,证明pop或者扩容导致的,重新从①开始
    }

    final void growArray() {
        // 如果工作队列还有元素,新建一个容量翻翻的新队列,将原队列中的元素移到新队列
    }
}
