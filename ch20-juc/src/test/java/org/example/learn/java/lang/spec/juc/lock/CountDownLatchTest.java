package org.example.learn.java.lang.spec.juc.lock;

import org.example.learn.java.lang.spec.juc.util.ThreadUtils;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CountDownLatchTest {

    static class CountDownWorker {
        static final AtomicInteger idGenerator = new AtomicInteger(1);

        private final CountDownLatch countDownLatch;
        private final Thread thread;

        public CountDownWorker(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            this.thread = new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                countDownLatch.countDown();
            }, this.getClass().getSimpleName() + idGenerator.getAndIncrement());
        }

        public void start() {
            this.thread.start();
        }
    }

    static class CountDownAwaitWorker {
        static final AtomicInteger idGenerator = new AtomicInteger(1);

        private final CountDownLatch countDownLatch;
        private final Thread thread;

        public CountDownAwaitWorker(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            this.thread = new Thread(() -> {
                try {
                    System.out.printf("thread[%s] enter to await\n", Thread.currentThread().getName());
                    countDownLatch.await();
                    System.out.printf("thread[%s] return from await\n", Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, this.getClass().getSimpleName() + idGenerator.getAndIncrement());
        }

        public void start() {
            this.thread.start();
        }

        public void join() throws InterruptedException {
            this.thread.join();
        }
    }

    @Test
    public void test0() throws InterruptedException {
        int count = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(count);

        CountDownAwaitWorker awaitWorker = new CountDownAwaitWorker(countDownLatch);
        awaitWorker.start();

        for (int i = 0; i < count; i++) {
            new CountDownWorker(countDownLatch).start();
        }

        System.out.println("main thread enter to join");
        awaitWorker.join();
        System.out.println("main thread return from join");
    }

    @Test
    public void test1() throws InterruptedException {
        int count = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(count);

        CountDownAwaitWorker awaitWorker1 = new CountDownAwaitWorker(countDownLatch);
        CountDownAwaitWorker awaitWorker2 = new CountDownAwaitWorker(countDownLatch);
        awaitWorker1.start();
        awaitWorker2.start(); // awaitWorker的线程在同步队列中挂起等待
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);

        for (int i = 0; i < count; i++) {
            new CountDownWorker(countDownLatch).start(); // 最后一个countdown才会唤醒第一个等待线程,第一个线程在获取资源后再使用级联唤醒第二个等待线程,依次类推,唤醒所有的等待线程
        }

        System.out.println("main thread enter to join");
        awaitWorker1.join();
        awaitWorker2.join();
        System.out.println("main thread return from join");

        CountDownAwaitWorker awaitWorker3 = new CountDownAwaitWorker(countDownLatch);
        awaitWorker3.start(); // countdown变为0后,await方法不再阻塞线程.
    }
}
