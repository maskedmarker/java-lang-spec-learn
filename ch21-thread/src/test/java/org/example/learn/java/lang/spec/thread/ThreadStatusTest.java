package org.example.learn.java.lang.spec.thread;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadStatusTest {

    /**
     * 列举常见的Thread.Status new/runnable/terminated
     */
    @Test
    public void test0() throws InterruptedException {
        Thread workerThread = new Thread("worker-thread");
        System.out.printf("创建线程thread[%s],并未start该thread\n", workerThread.getName());
        System.out.println("workerThreadState = " + workerThread.getState());
        Assert.assertTrue("thread刚创建且并未start前,状态为new", Thread.State.NEW.equals(workerThread.getState()));

        workerThread.start();
        System.out.printf("start该thread[%s]\n", workerThread.getName());
        System.out.println("workerThreadState = " + workerThread.getState());
        Assert.assertTrue("thread在start后,状态为runnable", Thread.State.RUNNABLE.equals(workerThread.getState()));

        // 等待workerThread结束
        while (workerThread.isAlive()) {
            Thread.yield();
        }

        System.out.println("workerThreadState = " + workerThread.getState());
        Assert.assertTrue("thread结束后,状态为terminated", Thread.State.TERMINATED.equals(workerThread.getState()));
    }

    /**
     * Thread.Status.BLOCKED也是可以被观测到的
     */
    @Test
    public void test1() throws InterruptedException {
        final Object lock = new Object();
        final AtomicBoolean hasGotLock = new AtomicBoolean(false);
        final AtomicBoolean readyToReleaseLock = new AtomicBoolean(false);

        Thread holdingLockThread = new Thread("worker-thread-1"){
            @Override
            public void run() {
                try {
                    // worker-thread-1通过判断hasGotLock从而避免因为获取monitor而被阻塞
                    while (hasGotLock.get()) {
                        Thread.yield();
                    }

                    System.out.printf("thread[%s] is running | isInterrupted=%s\n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                    synchronized (lock) {
                        System.out.printf("thread[%s] has got synchronized lock \n", Thread.currentThread().getName());
                        hasGotLock.set(true);

                        // 等待通知,然后跳出synchronized块
                        while (!readyToReleaseLock.get()) {
                            Thread.yield();
                        }
                    }

                    System.out.printf("thread[%s] is at the end of running | isInterrupted=%s\n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Thread acquiringLockThread = new Thread("worker-thread-2") {
            @Override
            public void run() {
                // worker-thread-2通过判断hasGotLock从而故意因为获取monitor而被阻塞
                while (!hasGotLock.get()) {
                    Thread.yield();
                }

                System.out.printf("before acquiring monitor, thread[%s] is running | isInterrupted=%s\n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                synchronized (lock) {
                    System.out.printf("thread[%s] has got synchronized lock  | isInterrupted=%s\n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                }

                System.out.printf("thread[%s] is at the end of running | isInterrupted=%s\n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
            }
        };


        // 因为有hasGotLock变量控制,所以worker-thread-1先获取到monitor,然后worker-thread-2再去获取monitor.worker-thread-2再去获取monitor会被阻塞
        holdingLockThread.start();
        acquiringLockThread.start();

        // 等待一会再观测线程状态
        TimeUnit.SECONDS.sleep(3);
        System.out.println("holdingLockThread status = " + holdingLockThread.getState());
        System.out.println("acquiringLockThread status = " + acquiringLockThread.getState());
    }
}
