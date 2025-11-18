package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 并发线程数不能大于size,否则锁机制就出问题
 *
 * 相对于多个线程都在盯着同一个内存地址spin来说,ALock中每个线程在spin时盯着的内存地址是不同的
 * 如果要再优化的,需要考虑cpu的cache-line的大小,通过在整数组中添加padding元素,来降低false-sharing
 */
public class ALock implements Lock {

    ThreadLocal<Integer> mySlotIndex = ThreadLocal.withInitial(() -> 0);

    AtomicInteger tail;

    int size;
    boolean[] lockedFlag;

    /**
     * 只支持capacity个并发线程,否则就出问题了
     */
    public ALock(int capacity) {
        size = capacity;
        tail = new AtomicInteger(0);
        lockedFlag = new boolean[capacity]; // 此时数组flag各个元素都是false(false表示未获取到锁)
        lockedFlag[0] = true; // 为了让第一个线程自动获取锁,所以首个线程的slot必须是true
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

    /**
     * 假定size==3,flag只能容下3个元素
     * 第1个线程的slot为0,此时flag[0]==true,直接获得锁(此时tail==1)
     * 第2个线程的slot为1,此时flag[1]==false,线程等待;
     * 第3个线程的slot为2,此时flag[2]==false,线程等待;
     * (在第1个线程释放锁之前,不支持更大的并发线程数)
     * 当第1个线程释放锁时,flag[0]:=false,flag[1]:=true;第2个线程获得锁
     * 第4个线程的slot为0,此时flag[0]==false,线程等待;
     * 当第2个线程释放锁时,flag[1]:=false,flag[2]:=true;第3个线程获得锁
     * 当第3个线程释放锁时,flag[2]:=false,flag[0]:=true;第4个线程获得锁
     * ...
     */
    @Override
    public void lock() {
        int slot = tail.getAndIncrement() % size;
        mySlotIndex.set(slot);
        // 并发线程数不能大于size,否则这里就出问题了
        while (!lockedFlag[slot]) {
            // busy-spin
        }

    }

    @Override
    public void unlock() {
        int slot = mySlotIndex.get();
        lockedFlag[slot] = false;
        lockedFlag[(slot + 1) % size] = true; // TODO 这里没有考虑到write的时效性,可能过了好久新值才同步到read-cpu
    }
}
