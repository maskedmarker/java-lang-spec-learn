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
            lock.lock(); // 锁的status+1
            try {
                System.out.println("Thread1 waiting");
                condition.await();  // 当前线程进入条件队列，锁的status-1,然后唤醒同步队列的头节点的线程,挂起当前线程
                System.out.println("Thread1 resumed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock(); // 锁的status-1,然后唤醒同步队列的头节点的线程
            }
        });

        Thread t2 = new Thread(() -> {
            lock.lock(); // 锁的status+1
            try {
                System.out.println("Thread2 signaling");
                condition.signal(); // 将条件队列中的header转移到同步队列,并将其对应的线程唤醒. 注意:这里并没有锁的status-1,也没有挂起当前线程
            } finally {
                lock.unlock(); // 锁的status-1,然后唤醒同步队列的头节点的线程
            }
        });

        t1.start();
        t2.start();
    }
}
