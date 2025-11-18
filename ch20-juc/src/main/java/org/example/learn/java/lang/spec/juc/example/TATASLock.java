package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * TATAS == test and test-and-set
 */
public class TATASLock implements Lock {

    AtomicBoolean locked = new AtomicBoolean(false);

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

    @Override
    public void lock() {
        while (true) {
            // 就是这小小的逻辑改善,在高并发场景下导致巨大的性能改善
            // volatile-read可以读取cpu cache的值,这种spin被称为local-spinning
            while (locked.get()) {
                if (!locked.getAndSet(true)) {
                    return;
                }
            }
        }
    }


    @Override
    public void unlock() {
        locked.set(false);
    }
}
