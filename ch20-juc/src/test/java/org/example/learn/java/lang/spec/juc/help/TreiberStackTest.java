package org.example.learn.java.lang.spec.juc.help;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Treiber Stack 是一种著名的无锁（lock-free）栈（stack）实现，由 Robert Treiber 在 1986 年提出。
 * 这种数据结构是多线程环境下实现 高性能并发栈 的典型代表，广泛应用于操作系统、并发库、虚拟机等场景中。
 */
public class TreiberStackTest {

    static class TreiberStack<T> {

        private static class Node<T> {
            final T value;
            Node<T> next;
            Node(T value) { this.value = value; }
        }

        private final AtomicReference<Node<T>> head = new AtomicReference<>();

        public void push(T value) {
            Node<T> newHead = new Node<>(value);
            Node<T> oldHead;
            do {
                oldHead = head.get();
                newHead.next = oldHead;
            } while (!head.compareAndSet(oldHead, newHead));
        }

        public T pop() {
            Node<T> oldHead;
            Node<T> newHead;
            do {
                oldHead = head.get();
                if (oldHead == null) return null;
                newHead = oldHead.next;
            } while (!head.compareAndSet(oldHead, newHead));
            return oldHead.value;
        }
    }

}
