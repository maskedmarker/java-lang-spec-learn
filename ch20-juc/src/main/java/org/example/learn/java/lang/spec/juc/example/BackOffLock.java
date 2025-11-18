package org.example.learn.java.lang.spec.juc.example;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class BackOffLock implements Lock {

    private static final int MIN_DELAY = 100;
    private static final int MAX_DELAY = 300;

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
            Backoff backoff = new Backoff(MIN_DELAY, MAX_DELAY);
            while (locked.get()) {
                if (!locked.getAndSet(true)) {
                    return;
                } else {
                    try {
                        backoff.backoff();
                    } catch (InterruptedException e) {
                        // ignore  do not support interruption
                    }
                }
            }
        }
    }


    @Override
    public void unlock() {
        locked.set(false);
    }


    public class Backoff {

        final int minDelay, maxDelay;
        int limit;
        final Random random;

        public Backoff(int min, int max) {
            minDelay = min;
            maxDelay = min;
            limit = minDelay;
            random = new Random();
        }

        public void backoff() throws InterruptedException {
            int delay = random.nextInt(limit);
            limit = Math.min(maxDelay, 2 * limit);
            Thread.sleep(delay);
        }
    }
}
