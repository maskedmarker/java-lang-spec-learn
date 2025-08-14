package org.example.learn.java.lang.spec.juc.thread;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * 注意区分
 * isInterrupted()是实例方法
 * interrupted()是类方法,判断的是当前线程
 */
public class ThreadInterruptTest {

    @Test
    public void test0() {
        Thread myThread = new Thread("my-thread"){
            @Override
            public void run() {
                System.out.println(this.getName() + "is running");
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    // nop
                }
                System.out.println(this.getName() + "is at the end of running");
            }
        };
        myThread.start();

        try {
            TimeUnit.NANOSECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        myThread.interrupt();
        System.out.println("myThread.interrupt() = " + myThread.isInterrupted());
    }

    /**
     * 抛出InterruptedException异常时,interrupt-status将不再赋值
     */
    @Test
    public void test1() {
        Thread myThread = new Thread("my-thread"){
            @Override
            public void run() {
                while (true) {
                    System.out.println(this.getName() + "is running");
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        myThread.start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        myThread.interrupt();
        System.out.println("myThread.isInterrupted() = " + myThread.isInterrupted());
    }
}
