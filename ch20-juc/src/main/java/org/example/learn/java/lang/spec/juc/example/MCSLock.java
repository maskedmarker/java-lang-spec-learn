package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 *
 */
public class MCSLock implements Lock {

    class QNode {
        // true表示等待,false表示持有锁
        volatile boolean locked;
        volatile QNode next;
    }

    AtomicReference<QNode> tail;
    ThreadLocal<QNode> myNode;

    public MCSLock() {
        tail = new AtomicReference<QNode>(null);
        myNode = ThreadLocal.withInitial(() -> new QNode());
    }

    @Override
    public void lock() {
        QNode qnode = myNode.get();
        // 通过CAS设置queue的队尾
        // 当返回为null时,即之前队列中没有节点,当前线程的节点是队列的首节点,可以获取锁
        QNode pred = tail.getAndSet(qnode);

        // 当pred为null时,即当前线程的节点是队列的首节点,可以获取锁
        // 当pred不为null时,即当前线程的节点不是队列的首节点,需要等待
        if (pred != null) {
            // 设置自己节点设置为true,表示等待
            qnode.locked = true;
            // 通过设置next来完成显示的队列queue
            pred.next = qnode;
            // wait until predecessor gives up the lock
            while (qnode.locked) {
            }
        }
    }

    @Override
    public void unlock() {
        QNode qnode = myNode.get();
        // [似乎]首节点没有后续节点
        if (qnode.next == null) {
            // 如果通过CAS设置成功tail,则证明确实没有其他节点了
            if (tail.compareAndSet(qnode, null))
                return;
            // 如果CAS失败,表明刚才[似乎]首节点没有后续节点现在确定有了新增节点加入,需要等待新节点执行完lock方法(设置好next)
            while (qnode.next == null) {
            }
        }

        // 当前线程通知下个节点的线程,不用等待(持有锁)
        qnode.next.locked = false;
        qnode.next = null;
        // 最好再设置 qnode.locked=false 方便回收再利用qnode
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock(long arg0, TimeUnit arg1) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
