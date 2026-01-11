package org.example.learn.java.lang.spec.juc.synchronizer.barrier;

import org.junit.Test;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

public class PhaserTest3 {
    
    private static final int NUMBER_OF_WORKER = 3;

    private static class Worker implements Runnable{

        final int threadId;
        final long workTimeSec;
        final Phaser phaser;

        public Worker(int threadId, long workTimeSec, Phaser phaser) {
            this.threadId = threadId;
            this.workTimeSec = workTimeSec;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            int phase = -1;
            try {
                while (!phaser.isTerminated()) {
                    phase = phaser.getPhase();
                    System.out.printf("线程[%d]开始阶段[%d]\n", threadId, phase);
                    Thread.sleep(workTimeSec * 1000L); // 模拟不同耗时的工作
                    phase = phaser.arriveAndAwaitAdvance();
                    System.out.printf("线程[%d]完成阶段[%d],等待其他线程...\n", threadId, phase); // 到达并等待前进
                }

                System.out.printf("线程[%d]结束工作,最终处于阶段[%d]\n", threadId, phase);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  Phaser默认情况下
     */
    @Test(timeout = 10 * 1000)
    public void test01() throws InterruptedException {
        // 创建屏障
        Phaser phaser = new Phaser(3);

        Thread[] workers = new Thread[NUMBER_OF_WORKER];
        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            Thread worker = new Thread(new Worker(i, i, phaser));
            workers[i] = worker;
            worker.start();
        }

        // 等待所有工作线程结束
        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            workers[i].join();
        }
        System.out.println("all worker has done job !!!");
    }

    @Test
    public void test02() throws InterruptedException {
        // 创建屏障
        Phaser phaser = new Phaser(3);

        Thread[] workers = new Thread[NUMBER_OF_WORKER];
        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            Thread worker = new Thread(new Worker(i, i, phaser));
            workers[i] = worker;
            worker.start();
        }

        // 等待一会
        TimeUnit.SECONDS.sleep(10);

        // 工作线程会无穷执行,强制结束
        phaser.forceTermination();
        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            workers[i].join(TimeUnit.SECONDS.toMillis(1));
        }
        System.out.println("phaser terminated at phase " +  (phaser.getPhase() & Integer.MAX_VALUE));  // phaser终止后,需要剔除31-bit的值
    }



    private static class IllWorker extends Worker {

        public IllWorker(int threadId, long workTimeSec, Phaser phaser) {
            super(threadId, workTimeSec, phaser);

        }

        @Override
        public void run() {
            int phase;
            try {
                System.out.printf("线程[%d]开始第1阶段\n", threadId);
                Thread.sleep(threadId * 1000L); // 模拟不同耗时的工作
                System.out.printf("线程[%d]完成第1阶段,等待其他线程...\n", threadId);
                phase = phaser.arriveAndAwaitAdvance(); // 到达并等待前进

                System.out.printf("线程[%d]开始第2阶段\n", threadId);
                Thread.sleep(threadId * 500L); // 模拟不同耗时的工作
                System.out.printf("线程[%d]完成第2阶段,等待其他线程...\n", threadId);
                phaser.arrive(); // 到达并等待前进

                System.out.printf("线程[%d]开始第3阶段\n", threadId);
                Thread.sleep(threadId * 200L); // 模拟不同耗时的工作
                System.out.printf("线程[%d]完成第3阶段,等待其他线程...\n", threadId);
                phaser.arriveAndAwaitAdvance(); // 到达并等待前进

                System.out.printf("线程[%d]结束工作\n", threadId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test1() throws InterruptedException {
        // 创建屏障
        Phaser phaser = new Phaser(3);

        Thread[] workers = new Thread[NUMBER_OF_WORKER];
        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            final int threadId = i;
            Thread worker = new Thread(() -> {
                try {
                    System.out.printf("线程[%d]开始第1阶段\n", threadId);
                    Thread.sleep(threadId * 1000L); // 模拟不同耗时的工作
                    System.out.printf("线程[%d]完成第1阶段,等待其他线程...\n", threadId);
                    phaser.arriveAndAwaitAdvance(); // 到达并等待前进

                    System.out.printf("线程[%d]开始第2阶段\n", threadId);
                    Thread.sleep(threadId * 500L); // 模拟不同耗时的工作
                    System.out.printf("线程[%d]完成第2阶段,等待其他线程...\n", threadId);
                    phaser.arriveAndAwaitAdvance(); // 到达并等待前进

                    System.out.printf("线程[%d]开始第3阶段\n", threadId);
                    Thread.sleep(threadId * 200L); // 模拟不同耗时的工作
                    System.out.printf("线程[%d]完成第3阶段,等待其他线程...\n", threadId);
                    phaser.arriveAndAwaitAdvance(); // 到达并等待前进

                    System.out.printf("线程[%d]结束工作\n", threadId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            workers[i] = worker;
            worker.start();
        }

        // 等待所有工作线程结束
        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            workers[i].join();
        }
    }
}
