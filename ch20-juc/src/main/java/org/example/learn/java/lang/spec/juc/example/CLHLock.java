package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 *  因为ALock需要提前知道并发线程数的最大值,CLHLock通过隐式queue支持无穷大的并发线程数
 *  与ALock一样,每个线程在spin时盯着的内存地址是不同的
 *
 *
 *  注意: 描述的边界,这里讨论的是锁的实现,而非站在锁的使用方的视角
 *
 *  利用入队的CAS原子操作将线程按时间排序.
 */
public class CLHLock implements Lock {

    // ⚠️这个queue到底是容纳什么的队列??
    // 队列容纳的是时间线上后面线程看到的锁状态.通过CAS队尾将并发线程排好时间先后.前节点的locked值是当前节点线程看到的锁状态,而当前节点的locked值是当前节点线程希望后节点线程看到的锁状态.
    // 为了保证锁只有一个线程持有,必须保证队列中只有一个锁状态是unlocked其余都是locked,最靠前的线程看到是unlocked,后续线程看到的是locked.
    class QNode {
        // true  => predecessor still holds/claims lock;false => predecessor has released lock
        volatile boolean locked = false;
    }

    AtomicReference<QNode> tail;
    ThreadLocal<QNode> myPred;
    ThreadLocal<QNode> myNode;

    public CLHLock() {
        tail = new AtomicReference<QNode>(new QNode()); // 锁的初始状态用一个dummy-node(locked=false)表示
        myNode = ThreadLocal.withInitial(() -> new QNode());
        myPred = ThreadLocal.withInitial(() -> null);
    }

    @Override
    public void lock() {
        // 获得初始化的值,或者被设置的值
        QNode qnode = myNode.get();
        // 让后续线程看到锁已经locked
        qnode.locked = true;

        // CAS操作来完成在队尾入队,并原子性获取到原队尾
        QNode pred = tail.getAndSet(qnode);
        // 并通过ThreadLocal的形式,将这些node串联成隐式的queue
        myPred.set(pred);

        // (前节点的锁状态是当前线程看到的锁状态)等待锁释放.(第一个线程看到锁的初始状态是unlocked,直接不用等待)
        while (pred.locked) {
            // spin
        }
    }

    @Override
    public void unlock() {
        QNode qnode = myNode.get();
        // 释放锁(即让后节点看到锁已经unlocked)
        qnode.locked = false;

        // 锁已经释放了,myPred代表当前线程看到的之前的锁状态,已经无用了.
        // 为了节省内存开销实现循环使用,将前节点回收利用
        myNode.set(myPred.get());
        // 如果更优雅点, 还需要主动设置myPred.set(null) 尽管lock中会重新设置正确的值
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
