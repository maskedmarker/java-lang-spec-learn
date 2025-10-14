package org.example.learn.java.lang.spec.thread;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 注意区分
 * isInterrupted()是实例方法
 * interrupted()是类方法,判断的是当前线程
 *
 * 线程在alive状态下,isInterrupted()才会返回正确的中断状态.当线程结束后,该实例方法返回的永远是false
 */
public class ThreadInterruptTest {

    /**
     * 正常运行状态下(没有进入monitor/正在加锁/sleep)
     */
    @Test
    public void test0() {
        final AtomicBoolean computingStarted = new AtomicBoolean(false);

        Thread workerThread = new Thread("worker-thread"){
            @Override
            public void run() {
                System.out.println(this.getName() + "is running");
                computingStarted.set(true);

                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    // nop
                }
                System.out.printf("thread[%s].isInterrupted() = %s\n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());

                // 上面仅仅是简单的计算,不受中断影响,所以这行代码只有for循环正常执行完后才会执行
                System.out.println(this.getName() + "is at the end of running");
            }
        };
        workerThread.start();

        System.out.printf("thread[%s]等待thread[%s]开始工作\n", Thread.currentThread().getName(), workerThread.getName());
        while (!computingStarted.get()) {
            Thread.yield();
        }

        System.out.printf("thread[%s]已经开始工作,再中断它\n", workerThread.getName());
        workerThread.interrupt();

        // 象征性的等一下,再查看状态
        try {
            TimeUnit.NANOSECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("%s isInterrupted ? %s | thread is alive ? %s \n", workerThread.getName(), workerThread.isInterrupted(), workerThread.isAlive());
        Assert.assertTrue("isInterrupted()是实例方法,检测线程对象的interrupt-status,且调用该方法不会改变该状态位", workerThread.isInterrupted());

        try {
            workerThread.join();
            System.out.printf("等待thread[%s] 结束后,再次查看该线程的中断状态\n", workerThread.getName());
            Assert.assertFalse("线程结束后,isInterrupted()返回的永远是false", workerThread.isInterrupted());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * sleep时抛出InterruptedException异常时,interrupt-status就不会再赋值了
     */
    @Test
    public void test1() {
        final AtomicBoolean workerReadyToSleep = new AtomicBoolean(false);
        final AtomicBoolean workerReadyToDie = new AtomicBoolean(false);

        Thread workerThread = new Thread("worker-thread"){
            @Override
            public void run() {
                System.out.printf("thread[%s] is running\n", Thread.currentThread().getName());
                try {
                    workerReadyToSleep.set(true);
                    TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    System.out.printf("while throwing InterruptedException, thread[%s] isInterrupted() = %s\n", Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
                    throw new RuntimeException(e);
                }

                // 防止线程提前结束
                while (!workerReadyToDie.get()) {
                    Thread.yield();
                }
                System.out.println(this.getName() + "is at the end of running");
            }
        };
        workerThread.start();

        try {
            System.out.printf("thread[%s]等待thread[%s]开始sleep\n", Thread.currentThread().getName(), workerThread.getName());
            while (!workerReadyToSleep.get()) {
                Thread.yield();
            }

            // 象征性的等一下,再查看状态
            TimeUnit.SECONDS.sleep(1);
            System.out.printf("thread[%s] is ready to sleep, thread[%s] try to interrupt it\n", workerThread.getName(), Thread.currentThread().getName());
            workerThread.interrupt();

            System.out.printf("%s isInterrupted ? %s | thread is alive ? %s \n", workerThread.getName(), workerThread.isInterrupted(), workerThread.isAlive());
            Assert.assertFalse("thread在sleep时被interrupt,线程抛出InterruptedException表示sleep是被中断而结束的,此时interrupt-status不会赋值(抛异常已经表示了中断行为,再赋值就重复了)", workerThread.isInterrupted());

            System.out.printf("thread[%s] 通知 thread[%s]可以结束了\n", Thread.currentThread().getName(), workerThread.getName());
            workerReadyToDie.set(true);
            // 象征性的等一下,再查看状态
            TimeUnit.SECONDS.sleep(1);
            Assert.assertFalse("thread在sleep时被interrupt,线程抛出InterruptedException表示sleep是被中断而结束的,此时interrupt-status不会被清空", workerThread.isInterrupted());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
