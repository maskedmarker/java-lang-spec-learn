package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.TimeUnit;
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
 *  利用队列的有序性将并发的线程排序,靠前的线程先获得锁
 *
 *  注意: CLHLock是原版,CLHLock2是精简版
 */
public class CLHLock2 implements Lock {

    // ⚠️这个queue到底是容纳什么的队列??
    // 队列节点的先后顺序容纳的是并发线程抢夺锁的优先顺序;(核心功能)
    // 队列节点的locked字段容纳的是线程对锁的持有状态:线程是否已经释放锁了,还是说正在等待或者已经持有锁;(核心功能)
    // 队列节点的thread字段容纳的是线程与其节点的映射关系(辅助功能)



    class QNode {

        // 将状态分的更细致
        static final int STATUS_RELEASED = -1;
        static final int STATUS_WAITING = 0;
        static final int STATUS_ACQUIRED = 1;


        // ⚠️ 注意locked字段的解释,尤其是"or is waiting for the lock"
        // If the field is -1, then the thread has released the lock.
        // If the field is 0, then the corresponding thread is waiting for the lock.
        // If the field is 1, then the corresponding thread has either acquired the lock.
        volatile int status;

        volatile Thread thread;

        // 通过prev字段将节点连成FIFO的队列
        volatile QNode prev;

        void reset(){
            this.status = -2;
            this.thread = null;
            this.prev = null;
        }
    }

    // 对FIFO队列的引用
    final AtomicReference<QNode> tail;

    // 保存线程与QNode的映射关系
    final ThreadLocal<QNode> qNodeKeeper = ThreadLocal.withInitial(QNode::new);

    public CLHLock2() {
        QNode initStatus = new QNode();
        initStatus.status = QNode.STATUS_RELEASED;
        tail = new AtomicReference<QNode>(initStatus); // 锁需要一个dummy-node(locked=false)表示初始状态(锁已被释放)
    }

    @Override
    public void lock() {
        QNode qnode = qNodeKeeper.get();
        // 等待锁
        qnode.status = QNode.STATUS_WAITING;

        // CAS操作来完成在队尾入队,并原子性获取到原队尾
        QNode pred = tail.getAndSet(qnode);
        // 设置前节点
        qnode.prev = pred;

        // [按照入队的先后顺序]等待前节点释放锁
        while (pred.status >= 0) {
            // spin
        }
        // 已经获得锁
        qnode.status = QNode.STATUS_ACQUIRED;
    }

    @Override
    public void unlock() {
        // 在unlock时,将当前线程节点当作新的头节点(保持逻辑dummy-node)
        QNode qNode = qNodeKeeper.get();
        QNode prev = qNode.prev;
        // 需要清除线程节点的无用状态
        qNode.prev = null;
        qNode.thread = null;

        // 释放锁
        qNode.status = QNode.STATUS_RELEASED;

        // 为了节省内存开销实现循环使用,将前节点回收利用
        prev.reset();
        qNodeKeeper.set(prev);
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
