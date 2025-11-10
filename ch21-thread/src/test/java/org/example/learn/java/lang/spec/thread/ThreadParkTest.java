package org.example.learn.java.lang.spec.thread;

import org.example.learn.java.lang.spec.thread.util.ThreadUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * park()和parkNanos(timeout) 与wait()和wait(timeout)的线程状态一样
 *
 * 如果线程在调用 LockSupport.park() 时已经被中断，或者在 park() 期间被中断， 那么该 park() 调用会立即返回（不再阻塞）。park()不会自动清除中断标志
 */
public class ThreadParkTest {

    /**
     * 线程park挂起有限时间,被阻塞时的线程状态为TIMED_WAITING
     */
    @Test(timeout = 5 * 1000)
    public void test01() {
        Thread workerThread = new Thread(() -> {
            try {
                System.out.printf("thread[%s] 通过park挂起一小时 at %s \n", Thread.currentThread().getName(), Calendar.getInstance().getTime());
                LockSupport.parkNanos(TimeUnit.HOURS.toNanos(1));
                System.out.printf("thread[%s] 结束 at %s \n", Thread.currentThread().getName(), Calendar.getInstance().getTime());
            } catch (Exception e) {
                System.out.printf("thread[%s] logs %s \n", Thread.currentThread().getName(), e);
            }
        });
        workerThread.start();

        // 等待1秒
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
        Assert.assertEquals("被park的线程的状态应该为TIMED_WAITING, 状态与wait(timeout)相同", Thread.State.TIMED_WAITING, workerThread.getState());

        try {
            System.out.printf("开始join thread[%s] at %s \n", workerThread.getName(), Calendar.getInstance().getTime());
            // 如果workerThread被中断,会导致join()提前退出
            workerThread.join();
            System.out.printf("结束join thread[%s] at %s \n", workerThread.getName(), Calendar.getInstance().getTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 线程park挂起无限时间,被阻塞时的线程状态为WAITING
     */
    @Test(timeout = 5 * 1000)
    public void test02() {
        Thread workerThread = new Thread(() -> {
            try {
                System.out.printf("thread[%s] 通过park挂起无穷长时间 at %s \n", Thread.currentThread().getName(), Calendar.getInstance().getTime());
                LockSupport.park();
                System.out.printf("thread[%s] 结束 at %s \n", Thread.currentThread().getName(), Calendar.getInstance().getTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        workerThread.start();

        // 等待1秒
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
        Assert.assertEquals("被park的线程的状态应该为WAITING, 状态与wait()相同", Thread.State.WAITING, workerThread.getState());

        try {
            System.out.printf("开始join thread[%s] at %s \n", workerThread.getName(), Calendar.getInstance().getTime());
            // 如果workerThread被中断,会导致join()提前退出
            workerThread.join();
            System.out.printf("结束join thread[%s] at %s \n", workerThread.getName(), Calendar.getInstance().getTime());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * park()会因为interrupt而提前结束
     * park()不会自动清除中断标志
     */
    @Test(timeout = 5 * 1000)
    public void test11() {
        long start = System.currentTimeMillis();
        // 再park
        System.out.printf("开始park thread[%s] (isInterrupted=%s) at %s \n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertFalse(Thread.currentThread().isInterrupted());
        LockSupport.park();
        System.out.printf("结束park thread[%s] (isInterrupted=%s) at %s \n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertFalse(Thread.currentThread().isInterrupted());
        Assert.assertTrue("没有中断的前提下,当前线程只会在junit的timeout时被中断,从而退出park", ((System.currentTimeMillis() - start) >= TimeUnit.SECONDS.toMillis(5)));
    }

    /**
     * park()会因为interrupt而提前结束
     * park()不会自动清除中断标志
     */
    @Test
    public void test12() {
        // 先中断自己
        Thread.currentThread().interrupt();
        long start = System.currentTimeMillis();
        // 再park
        System.out.printf("开始park thread[%s] (isInterrupted=%s) at %s \n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertTrue(Thread.currentThread().isInterrupted());
        LockSupport.park();
        System.out.printf("结束park thread[%s] (isInterrupted=%s) at %s \n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertTrue("在park()前或者中,中断当前线程,会导致park直接退出", ((System.currentTimeMillis() - start) < TimeUnit.MILLISECONDS.toMillis(100)));
        Assert.assertTrue("park()不会自动清除中断标志", Thread.currentThread().isInterrupted());
    }

    @Test
    public void test13() {
        Thread workerThread = new Thread(() -> {
            try {
                System.out.printf("thread[%s] 通过park挂起无穷长时间 at %s \n", Thread.currentThread().getName(), Calendar.getInstance().getTime());
                LockSupport.park();
                System.out.printf("thread[%s] 结束 at %s \n", Thread.currentThread().getName(), Calendar.getInstance().getTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        workerThread.start();

        // 等待1秒
        ThreadUtils.yieldWait(2, TimeUnit.SECONDS);
        Assert.assertEquals("被park的线程的状态应该为WAITING, 状态与wait()相同", Thread.State.WAITING, workerThread.getState());
        Assert.assertFalse(workerThread.isInterrupted());

        long start = System.currentTimeMillis();
        System.out.println("开始中断工作线程 at" + Calendar.getInstance().getTime());
        workerThread.interrupt();
        while (Thread.State.WAITING.equals(workerThread.getState())) {
            Thread.yield();
        }
        Assert.assertTrue("park()会因为interrupt而提前结束", ((System.currentTimeMillis() - start) < TimeUnit.MILLISECONDS.toMillis(100)));
    }




    /**
     * LockSupport.park() 是基于 许可机制（permit） 实现的:
     * 每个线程最多有 一个 permit
     * 如果没有 permit，park()就阻塞
     * unpark(Thread t)：给线程发一个 permit（如果线程阻塞，则唤醒）
     *
     * 提前unpark(),则park()时直接返回
     */
    @Test
    public void test21() {
        // 先unpark
        LockSupport.unpark(Thread.currentThread());
        long start = System.currentTimeMillis();
        // 再park
        System.out.printf("开始park thread[%s] (isInterrupted=%s) at %s \n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertFalse(Thread.currentThread().isInterrupted());
        LockSupport.park();
        System.out.printf("结束park thread[%s] (isInterrupted=%s) at %s \n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted(), Calendar.getInstance().getTime());
        Assert.assertTrue("在park()前已经unpark()线程,会导致调用park()时直接退出", ((System.currentTimeMillis() - start) < TimeUnit.MILLISECONDS.toMillis(100)));
        Assert.assertFalse(Thread.currentThread().isInterrupted());
    }
}
