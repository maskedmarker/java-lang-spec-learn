package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 *
 * AtomicBoolean有2个原子操作
 *
 *     public final boolean compareAndSet(boolean expect, boolean update) {
 *         int e = expect ? 1 : 0;
 *         int u = update ? 1 : 0;
 *         return unsafe.compareAndSwapInt(this, valueOffset, e, u);
 *     }
 *
 *     public final boolean getAndSet(boolean newValue) {
 *         boolean prev;
 *         do {
 *             prev = get();
 *         } while (!compareAndSet(prev, newValue));
 *         return prev;
 *     }
 */
public class SpinLock implements Lock {

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

    /**
     *
     * 我们希望通过cas(false,true)来完成抢占锁,
     * 但是getAndSet(true)的实现是cas(x,true)加返回x,x为原来的值.
     * 当x==false时,发生的就是我们期望的cas(false,true),
     * 那如果x==true,此时发生了我们不希望的cas(true,true),当前线程只能等持有锁的线程通过unlock将x重新设置为false,才会出现前一种情形.
     *
     * 如果getAndSet(true)返回false,证明state原来的值是false(锁未被占用),此时抢占锁成功退出spin
     * 如果getAndSet(true)返回true,证明state原来的值是true(锁正被占用),此时需要继续spin
     */
    @Override
    public void lock() {
        while (locked.getAndSet(true)) {
            // busy-spin
        }
    }

    /**
     * 另一个更加直观的版本
     */
//    @Override
//    public void lock() {
//        while (locked.compareAndSet(false, true)) {
//            // busy-spin
//        }
//    }


    @Override
    public void unlock() {
        locked.set(false);
    }
}
