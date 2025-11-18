package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TOLock implements Lock {

    static class QNode {
        public QNode pred = null;
    }

    static QNode AVAILABLE = new QNode();
    AtomicReference<QNode> tail;
    ThreadLocal<QNode> myNode;

    public TOLock() {
        tail = new AtomicReference<QNode>(null);
        myNode = ThreadLocal.withInitial(() -> new QNode());
    }

    @Override
    public void lock() {

    }

    @Override
    public void unlock() {
        QNode qnode = myNode.get();
        if (!tail.compareAndSet(qnode, null))
            qnode.pred = AVAILABLE;
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long patience = TimeUnit.MILLISECONDS.convert(time, unit);

        QNode qnode = new QNode();
        myNode.set(qnode);
        qnode.pred = null;
        QNode myPred = tail.getAndSet(qnode);
        if (myPred == null || myPred.pred == AVAILABLE) {
            return true;
        }
        while (System.currentTimeMillis() - startTime < patience) {
            QNode predPred = myPred.pred;
            if (predPred == AVAILABLE) {
                return true;
            } else if (predPred != null) {
                myPred = predPred;
            }
        }
        if (!tail.compareAndSet(qnode, myPred))
            qnode.pred = myPred;
        return false;
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
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
