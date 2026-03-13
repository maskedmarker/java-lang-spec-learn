package org.example.learn.java.lang.spec.juc.synchronizer.barrier;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * semaphore的permits可以在创建构造对象时设置,而且还可以在使用中调整permits的大小.
 *
 * 💯💯💯本质是AQS允许有意的调整state值
 * 可以通过AQS的tryRelease来调整AQS的state值, 也可以通过AQS.compareAndSetState来暴力调整state值, 就看使用AQS的类是否开放此功能
 */
public class SemaphoreTest {

    private static final int NUMBER_OF_THREAD = 10;

    /**
     * semaphore的permits可以在创建构造对象时设置
     */
    @Test(timeout = 5 * 1000)
    public void test01() throws InterruptedException {
        final int PERMITS = NUMBER_OF_THREAD >>> 1;  // 增大等待
        final Semaphore semaphore = new Semaphore(PERMITS);
        final AtomicInteger workingThreadNum = new AtomicInteger(0);  // 正在working的线程数

        Thread[] workers = new Thread[NUMBER_OF_THREAD];
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            final int threadId = i;
            Thread worker = new Thread(() -> {
                while (true) {
                    try {
                        // 开始 working
                        semaphore.acquire(1);
                        int count = workingThreadNum.incrementAndGet();
                        System.out.printf("thread-%d is working. workingThreadNum: %d  availablePermits:%d \n", threadId, count, semaphore.availablePermits());

                        // working
                        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(500));

                        // 由于semaphore最多只能分配有限个permits,所以只有有限个线程在working
                        Assert.assertTrue("同时working的线程数据不可能大于PERMITS", count <= PERMITS);

                        // 结束working
                        workingThreadNum.decrementAndGet();
                        semaphore.release(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
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
     * 使用中调整semaphore的permits
     *
     */
    @Test(timeout = 5 * 1000)
    public void test021() throws InterruptedException {
        final int PERMITS = NUMBER_OF_THREAD >>> 1;  // 增大等待
        final Semaphore semaphore = new Semaphore(PERMITS);
        final AtomicInteger workingThreadNum = new AtomicInteger(0);  // 正在working的线程数
        final CountDownLatch allThreadRunning = new CountDownLatch(NUMBER_OF_THREAD);

        Thread[] workers = new Thread[NUMBER_OF_THREAD];
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            final int threadId = i;
            Thread worker = new Thread(() -> {
                allThreadRunning.countDown();

                while (true) {
                    try {
                        // 开始 working
                        semaphore.acquire(1);
                        int count = workingThreadNum.incrementAndGet();
                        System.out.printf("thread-%d is working. workingThreadNum: %d  availablePermits:%d \n", threadId, count, semaphore.availablePermits());

                        // working
                        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(500));

                        // 结束working
                        workingThreadNum.decrementAndGet();
                        semaphore.release(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
            workers[i] = worker;
            worker.start();
        }

        // 等待所有线程开始运行后,调整permits
        allThreadRunning.await();
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(2));
        semaphore.release(NUMBER_OF_THREAD >>> 1);

        // 等待所有工作线程结束
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            workers[i].join();
        }
    }

    /**
     * 使用中调整semaphore的permits
     *
     */
    @Test(timeout = 5 * 1000)
    public void test022() throws InterruptedException {
        final int PERMITS = NUMBER_OF_THREAD >>> 1;  // 增大等待
        final Semaphore semaphore = new Semaphore(PERMITS);
        final AtomicInteger workingThreadNum = new AtomicInteger(0);  // 正在working的线程数
        final CountDownLatch allThreadRunning = new CountDownLatch(NUMBER_OF_THREAD);

        Thread[] workers = new Thread[NUMBER_OF_THREAD];
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            final int threadId = i;
            Thread worker = new Thread(() -> {
                while (true) {
                    try {
                        // 开始 working
                        semaphore.acquire(1);
                        int count = workingThreadNum.incrementAndGet();
                        System.out.printf("thread-%d is working. workingThreadNum: %d  availablePermits:%d \n", threadId, count, semaphore.availablePermits());

                        // working
                        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(500));

                        // 结束working
                        workingThreadNum.decrementAndGet();
                        semaphore.release(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            });
            workers[i] = worker;
            worker.start();
        }

        // 等待所有线程开始运行后,调整permits到0
        allThreadRunning.await();
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(3));
        // returns all permits that are 💯💯 immediately available.
        // 💯💯这里仅仅是将permits(即state)设置为0,可是已经发放出去的permits还在,持有permits的持有者在release时归还的permits还以重新被使用
        // 这就意味着可以保持PERMITS个线程working
        semaphore.drainPermits();


        // 等待所有工作线程结束
        for (int i = 0; i < NUMBER_OF_THREAD; i++) {
            workers[i].join();
        }
    }
}
