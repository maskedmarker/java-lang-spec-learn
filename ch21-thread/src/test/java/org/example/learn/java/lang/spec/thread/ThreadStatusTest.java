package org.example.learn.java.lang.spec.thread;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

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


    /**
     * 线程在进行I/O操作时,虽然耗时比较久,但仍然在逻辑上当作一个不可打断的操作(尤其是磁盘的读写操作)
     * 所以线程在I/O操作时,线程状态仍然为runnable
     *
     *
     * 在linux系统中, 将I/O操作时线程的状态细分为TASK_INTERRUPTIBLE/TASK_UNINTERRUPTIBLE
     * TASK_INTERRUPTIBLE主要针对  Read socket or pipe
     * TASK_UNINTERRUPTIBLE 主要针对 Read/write disk (filesystem I/O)
     * Non-blocking I/O使用的是TASK_RUNNING
     *
     * 如果希望I/O操作时可以被"中断",可以使用操作系统提供的non-blocking接口或者AIO接口
     */
    @Test
    public void test2() throws InterruptedException {

        Thread workerThread = new Thread("worker-thread"){
            @Override
            public void run() {
                try {
                    System.out.printf("thread[%s] is ready to read from std\n", Thread.currentThread().getName());
                    System.in.read();
                    System.out.printf("thread[%s] is at the end of running\n", Thread.currentThread().getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        workerThread.start();

        while (Thread.State.NEW.equals(workerThread.getState())) {
            Thread.yield();
        }
        TimeUnit.SECONDS.sleep(5);

        System.out.printf("thread[%s] status is %s\n", workerThread.getName(), workerThread.getState());
        Assert.assertTrue("线程在进行I/O操作时,其状态为runnable", Thread.State.RUNNABLE.equals(workerThread.getState()));
    }

    @Test
    public void test3() {
        final Object lock = new Object();

        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    System.out.printf("thread[%s] 主动放弃monitor,wait 10s \n", Thread.currentThread().getName());
                    try {
                        lock.wait(10 * 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, "worker-thread");
        workerThread.start();

        // 等待1秒
        long start = System.currentTimeMillis();
        long end = start + 1000;
        while (System.currentTimeMillis() < end) {
            Thread.yield();
        }

        System.out.printf("thread[%s] status is %s\n", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("wait(timeout)的线程的状态应该为TIMED_WAITING", Thread.State.TIMED_WAITING, workerThread.getState());
    }

    @Test
    public void test4() {
        final Object lock = new Object();

        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    System.out.printf("thread[%s] 主动放弃monitor,wait无穷时间 \n", Thread.currentThread().getName());
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, "worker-thread");
        workerThread.start();

        // 等待1秒
        long start = System.currentTimeMillis();
        long end = start + 1000;
        while (System.currentTimeMillis() < end) {
            Thread.yield();
        }

        System.out.printf("thread[%s] status is %s\n", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("wait()的线程的状态应该为WAITING", Thread.State.TIMED_WAITING, workerThread.getState());
    }

    @Test
    public void test5() {
        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // park挂起10秒
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(10));
            }
        }, "worker-thread");
        workerThread.start();

        // 等待1秒
        long start = System.currentTimeMillis();
        long end = start + 1000;
        while (System.currentTimeMillis() < end) {
            Thread.yield();
        }

        System.out.printf("thread[%s] status is %s\n", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("被park的线程的状态应该为TIMED_WAITING, 状态与wait(timeout)相同", Thread.State.TIMED_WAITING, workerThread.getState());
    }

    @Test
    public void test6() {
        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LockSupport.park();
            }
        }, "worker-thread");
        workerThread.start();

        // 等待1秒
        long start = System.currentTimeMillis();
        long end = start + 1000;
        while (System.currentTimeMillis() < end) {
            Thread.yield();
        }

        System.out.printf("thread[%s] status is %s\n", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("被park的线程的状态应该为WAITING, 状态与wait()相同", Thread.State.WAITING, workerThread.getState());
    }
}
