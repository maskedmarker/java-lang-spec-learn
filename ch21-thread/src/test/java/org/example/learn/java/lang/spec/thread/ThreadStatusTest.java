package org.example.learn.java.lang.spec.thread;

import org.example.learn.java.lang.spec.thread.util.LogUtils;
import org.example.learn.java.lang.spec.thread.util.ThreadUtils;
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
        Thread workerThread = new Thread(() -> {
            ThreadUtils.yieldWait(3, TimeUnit.SECONDS); // 防止过快结束
        },"worker-thread");

        LogUtils.log("创建线程thread[%s],并未start该thread", workerThread.getName());
        LogUtils.log("workerThreadState = " + workerThread.getState());
        Assert.assertEquals("thread刚创建且并未start前,状态为new", Thread.State.NEW, workerThread.getState());

        workerThread.start();
        LogUtils.log("start该thread[%s]", workerThread.getName());
        LogUtils.log("workerThreadState = " + workerThread.getState());
        Assert.assertEquals("thread在start后,状态为runnable", Thread.State.RUNNABLE, workerThread.getState());

        // 等待workerThread结束
        while (workerThread.isAlive()) {
            Thread.yield();
        }

        LogUtils.log("workerThread结束后, workerThreadState = " + workerThread.getState());
        Assert.assertTrue("thread结束后,状态为terminated", Thread.State.TERMINATED.equals(workerThread.getState()));
    }

    /**
     * Thread.Status.BLOCKED也是可以被观测到的
     */
    @Test
    public void test1() throws InterruptedException {
        final Object lock = new Object();

        Thread workerThread;
        synchronized (lock) {
            LogUtils.log("thread has got synchronized lock");

            workerThread = new Thread(() -> {
                LogUtils.log("before acquiring monitor, thread[%s] is running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                synchronized (lock) {
                    LogUtils.log("thread[%s] has got synchronized lock  | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                }

                LogUtils.log("thread[%s] is at the end of running | isInterrupted=%s", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
            }, "worker-thread");
            workerThread.start();


            ThreadUtils.yieldWait(5, TimeUnit.SECONDS);
            LogUtils.log("workerThread status = " + workerThread.getState());
            Assert.assertEquals("在当前线程释放锁之前,worker-thread被monitorenter阻塞", Thread.State.BLOCKED, workerThread.getState());
        }
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
                    LogUtils.log("thread[%s] is ready to read from std", Thread.currentThread().getName());
                    System.in.read();
                    LogUtils.log("thread[%s] is at the end of running", Thread.currentThread().getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        workerThread.start();

        while (Thread.State.NEW.equals(workerThread.getState())) {
            Thread.yield();
        }
        ThreadUtils.yieldWait(5, TimeUnit.SECONDS);
        LogUtils.log("thread[%s] status is %s", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("线程在进行I/O操作时,其状态为runnable", Thread.State.RUNNABLE, workerThread.getState());
    }

    @Test
    public void test3() {
        final Object lock = new Object();

        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    LogUtils.log("thread[%s] 主动放弃monitor,wait 10s ", Thread.currentThread().getName());
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
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);

        LogUtils.log("thread[%s] status is %s", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("wait(timeout)的线程的状态应该为TIMED_WAITING", Thread.State.TIMED_WAITING, workerThread.getState());
    }

    @Test
    public void test4() {
        final Object lock = new Object();

        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    LogUtils.log("thread[%s] 主动放弃monitor,wait无穷时间 ", Thread.currentThread().getName());
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
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);

        LogUtils.log("thread[%s] status is %s", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("wait()的线程的状态应该为WAITING", Thread.State.WAITING, workerThread.getState());
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
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);

        LogUtils.log("thread[%s] status is %s", workerThread.getName(), workerThread.getState());
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
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);

        LogUtils.log("thread[%s] status is %s", workerThread.getName(), workerThread.getState());
        Assert.assertEquals("被park的线程的状态应该为WAITING, 状态与wait()相同", Thread.State.WAITING, workerThread.getState());
    }
}
