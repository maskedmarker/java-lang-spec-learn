package org.example.learn.java.lang.spec.juc.park;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;


public class ParkTest {

    @Test(timeout = 5000)
    public void test() {
        // 没有线程unpark当前线程,因为无穷等待导致无法从park方法返回
        LockSupport.park(this);
    }

    @Test
    public void test1() {
        // 提前unpark当前线程
        LockSupport.unpark(Thread.currentThread());
        //当前线程被park时,可以consume前一个unpork给予的permit,无需等待就可以正常从park返回
        LockSupport.park(this);
    }

    /**
     * 退出park方法,
     *  有可能是被unpark了
     *  有可能是被interrupt了(因为没有抛出InterruptedException,就需要自己check interrupted status)
     *
     */
    @Test
    public void test2() {
        // 中断当前线程
        Thread.currentThread().interrupt();
        // 由于当前线程被interrupt,导致park无法挂起当前线程,正常从park返回,也不抛异常,也不消费interrupted status
        try {
            LockSupport.park(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // The interrupted status of the thread is unaffected by this method
        boolean interrupted = Thread.currentThread().isInterrupted();
        System.out.println("interrupted = " + interrupted);
    }

    @Test
    public void test3() throws InterruptedException {

        Thread t1 = new Thread(() -> {
            try {
                System.out.println("LockSupport.park() in t1");
                LockSupport.park();
                System.out.println("return from LockSupport.park() in t1");
                boolean interrupted = Thread.currentThread().isInterrupted();
                System.out.println("interrupted = " + interrupted + " in t1");
            } catch (Exception e) {
                System.out.println("in t1. e = " + e);
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
                System.out.println("t1.interrupt() by t2");
                t1.interrupt();
            } catch (Exception e) {
                System.out.println("in t2. e = " + e);
            }
        });

        t1.start();
        t2.start();

        System.out.println("t1.join() by main thread");
        t1.join();
    }
}
