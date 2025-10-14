package org.example.learn.java.lang.spec.juc.lock;

import org.junit.Test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {

    @Test
    public void test0() {
        Lock lock = new ReentrantLock();

        lock.lock();
        System.out.println("首次获取lock成功");
        try {
            System.out.println("do foo");

            lock.lock();
            System.out.println("再次获取lock成功");
            System.out.println("do bar");
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void test() {
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        Thread workerThread1 = new Thread(() -> {
            lock.lock(); // 锁的status+1
            try {
                System.out.printf("thread[%s] is waiting for condition\n", Thread.currentThread().getName());
                condition.await();  // 当前线程进入条件队列，锁的status-1,然后唤醒同步队列的头节点的线程,挂起当前线程
                System.out.printf("thread[%s] resumes while condition occurs\n", Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock(); // 锁的status-1,然后唤醒同步队列的头节点的线程
            }
        }, "worker-thread-1");

        Thread workerThread2 = new Thread(() -> {
            lock.lock(); // 锁的status+1
            try {
                System.out.printf("thread[%s] is signaling\n", Thread.currentThread().getName());
                condition.signal(); // 将条件队列中的header转移到同步队列,并将其对应的线程唤醒. 注意:这里并没有锁的status-1,也没有挂起当前线程
            } finally {
                lock.unlock(); // 锁的status-1,然后唤醒同步队列的头节点的线程
            }
        }, "worker-thread-2");

        workerThread1.start();
        workerThread2.start();
    }
}
