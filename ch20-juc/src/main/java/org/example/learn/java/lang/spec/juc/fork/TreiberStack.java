package org.example.learn.java.lang.spec.juc.fork;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Treiber 栈以其发明者 R. Kent Treiber 命名(发表于 1986 年的论文中).所有操作都围绕 Compare-And-Swap 循环展开.
 *
 * 特点与性质
 *      无锁(Lock-Free)
 *      非阻塞(Non-Blocking)
 *      ABA问题
 *          在更复杂的场景中,如果栈元素要被复用,单纯的Treiber栈就需要配合带标签的引用或危险指针等技术来防止ABA
 *
 *
 *  备注: ForkJoinPool使用了该算法的变体版本来持有idle-worker线程栈的头指针
 */
public class TreiberStack<T> {

    // 栈顶节点(原子引用)
    private AtomicReference<Node<T>> top = new AtomicReference<>();

    public void push(T value) {
        Node<T> newNode = new Node<>(value);
        while (true) {
            Node<T> currentTop = top.get();
            newNode.next = currentTop;
            // cas-top
            if (top.compareAndSet(currentTop, newNode)) {
                return;
            }
            // 如果cas-top,循环重试
        }
    }

    public T pop() {
        while (true) {
            Node<T> currentTop = top.get();
            if (currentTop == null) {
                return null;
            }
            Node<T> nextNode = currentTop.next;
            // cas-top
            if (top.compareAndSet(currentTop, nextNode)) {
                return currentTop.value;
            }
            // 如果cas-top,循环重试
        }
    }

    // 内部节点类
    private static class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }
}
