package org.example.learn.java.lang.spec.juc.synchronizer.barrier;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class CyclicBarrierTest {
    
    private static final int NUMBER_OF_THREAD = 3;

    @Test
    public void test0() throws InterruptedException {
        // 创建屏障
        Runnable barrierAction = () -> System.out.println("所有线程已到达屏障,执行屏障动作");
        CyclicBarrier barrier = new CyclicBarrier(NUMBER_OF_THREAD, barrierAction);

        Thread[] workers = new Thread[NUMBER_OF_THREAD];
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            final int threadId = i;
            Thread worker = new Thread(() -> {
                try {
                    System.out.printf("线程%d开始工作...\n", threadId);
                    Thread.sleep(threadId * 1000L); // 模拟不同耗时的工作

                    System.out.printf("线程%d到达屏障,等待其他线程...\n", threadId);
                    int index = barrier.await(); // 等待其他线程
                    System.out.printf("线程%d继续执行,它是第%d个到达的\n", threadId, (NUMBER_OF_THREAD - index));
                    // 如果没有发生超时/中断,当非最后一个到达的线程调用await方法会触发当前线程挂起;
                    // 最后一个到达的线程会先触发barrierAction,然后唤醒其他挂起的线程,更新CyclicBarrier的generation,然后释放锁并退出CyclicBarrier.await方法;
                    // 被唤醒的线程依次获取到锁后,发现generation变了,然后释放锁并退出CyclicBarrier.await方法.
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
            workers[i] = worker;
            worker.start();
        }

        // 等待所有工作线程结束
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            workers[i].join();
        }
    }


    /**
     * barrierAction不存在并发执行,最后一个到达的线程会在await方法中顺带执行barrierAction
     */
    @Test
    public void test1() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        // 创建屏障
        Runnable barrierAction = () -> counter.incrementAndGet();
        CyclicBarrier barrier = new CyclicBarrier(NUMBER_OF_THREAD, barrierAction);

        Thread[] workers = new Thread[NUMBER_OF_THREAD];
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            final int threadId = i;
            Thread worker = new Thread(() -> {
                try {
                    System.out.printf("线程%d开始工作...\n", threadId);
                    Thread.sleep(threadId * 1000L); // 模拟不同耗时的工作

                    System.out.printf("线程%d到达屏障,等待其他线程...\n", threadId);
                    int index = barrier.await(); // 等待其他线程
                    System.out.printf("线程%d继续执行,它是第%d个到达的\n", threadId, (NUMBER_OF_THREAD - index));
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
            workers[i] = worker;
            worker.start();
        }

        // 等待所有工作线程结束
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            workers[i].join();
        }
        Assert.assertEquals("barrierAction不存在并发执行,最后一个到达的线程会在await方法中顺带执行barrierAction", 1, counter.get());
    }

    /**
     * 当线程从await方法正常退出时,CyclicBarrier已经更新了generation,也就意味着可以重新使用CyclicBarrier
     */
    @Test
    public void test2() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        // 创建屏障
        Runnable barrierAction = () -> counter.incrementAndGet();
        CyclicBarrier barrier = new CyclicBarrier(NUMBER_OF_THREAD, barrierAction);

        Thread[] workers = new Thread[NUMBER_OF_THREAD];
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            final int threadId = i;
            Thread worker = new Thread(() -> {
                try {
                    System.out.printf("线程%d开始工作...\n", threadId);
                    Thread.sleep(threadId * 1000L); // 模拟不同耗时的工作

                    System.out.printf("线程%d到达屏障,等待其他线程...\n", threadId);
                    int index = barrier.await(); // 等待其他线程
                    System.out.printf("线程%d继续执行,它是第%d个到达的\n", threadId, (NUMBER_OF_THREAD - index));


                    // 当线程从await方法正常退出时,CyclicBarrier已经更新了generation,也就意味着可以重新使用CyclicBarrier
                    Thread.sleep(500);
                    System.out.printf("线程%d第二次到达屏障,等待其他线程...\n", threadId);
                    barrier.await();
                    System.out.println("线程" + threadId + "第二次通过屏障");
                    System.out.printf("线程%d第二次通过屏障\n", threadId);
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
            workers[i] = worker;
            worker.start();
        }

        // 等待所有工作线程结束
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            workers[i].join();
        }
        Assert.assertEquals("当线程从await方法退出时,CyclicBarrier已经更新了generation,也就意味着可以重新使用CyclicBarrier", 2, counter.get());
    }
}
