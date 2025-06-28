package org.example.learn.java.lang.spec.juc;

import org.junit.Test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {

    @Test
    public void test() {
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("Thread1 waiting");
                condition.await();  // 当前线程进入等待队列，并释放锁
                System.out.println("Thread1 resumed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        });

        Thread t2 = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("Thread2 signaling");
                condition.signal(); // 将condition队列中的node转移到等待队列,此时还未释放锁
            } finally {
                lock.unlock(); // 此时释放锁,并唤醒等待队列中一个线程
            }
        });

        t1.start();
        t2.start();
    }
}
