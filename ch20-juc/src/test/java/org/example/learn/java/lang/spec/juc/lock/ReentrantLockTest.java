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
}
