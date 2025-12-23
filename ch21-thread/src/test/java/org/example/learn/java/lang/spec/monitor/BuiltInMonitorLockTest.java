package org.example.learn.java.lang.spec.monitor;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * synchronized关键字使用的是内置监视器锁机制(built-in-monitor-lock)
 *
 *
 */
public class BuiltInMonitorLockTest {

    private int score = -1;
    private final Object syn = new Object();

    @Test
    public void test0() throws InterruptedException {
        score = 0;

        Thread[] workerThreads = new Thread[100];
        for (int i = 0; i < workerThreads.length; i++) {
            final int addScore = i;
            Thread workerThread = new Thread(() -> {
                addScore(addScore);
            });
            workerThread.start();

            workerThreads[i] = workerThread;
        }

        for (Thread workerThread : workerThreads) {
            workerThread.join();
        }
        int expectedScoreSum = IntStream.range(0, 100).sum();
        Assert.assertEquals(expectedScoreSum, score);
    }

    /**
     * 为了解释方便,假设抢占锁时,使用公平机制,同时忽略重入机制
     *
     *  每个线程先去同步队列排队等待,当成为队首的线程就会结束等待获得锁,使用完后,退出队列(也就释放了锁);
     *  队首后的下一个线程成为新的队首,然后就会获得锁结束等待,使用完后,退出队列(也就释放了锁);
     *  and so on;
     */
    void addScore(int delta) {
        synchronized (syn) {        // 去同步队列排队,等待成为同步队列队首(然后就会获取syn的monitor锁)
            score += delta;         // 执行临界区的代码 (临界区指的是会发生竟态问题的代码区域)
        }                           // 退出同步队列(也就释放了锁,同时下一个线程成为新的队首)
    }


    // ------------------------------------------------------------------------------------------------------------------

    @Test
    public void test1() throws InterruptedException {
        score = 0;

        class Worker implements Runnable {

            final int addScore;
            final int expectedScore;

            public Worker(int addScore) {
                this.addScore = addScore;
                this.expectedScore = IntStream.range(0, addScore).sum();
            }

            @Override
            public void run() {
                try {
                    doRun();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            private void doRun() throws InterruptedException {
                synchronized (syn) {                         // 去同步队列排队等待,等待成为同步队列队首(然后就会获取syn的monitor锁)
                    while (expectedScore != score) {
                        syn.wait();                          // 加入等待队列等待,退出同步队列(也就释放了锁,同时下一个线程成为新的队首); 当notifyAll调用时,将从等待队列退出进入同步队列等待成为队首
                    }

                    score += addScore;                      // 执行临界区的代码
                    syn.notifyAll();                        // 将等待队列的线程转移到同步队列
                }                                           // 退出同步队列(也就释放了锁,同时下一个线程成为新的队首)
            }
        }

        Thread[] workerThreads = new Thread[100];
        for (int i = 0; i < workerThreads.length; i++) {
            Thread workerThread = new Thread(new Worker(i));
            workerThread.start();

            workerThreads[i] = workerThread;
        }

        for (Thread workerThread : workerThreads) {
            workerThread.join();
        }
        int expectedScoreSum = IntStream.range(0, 100).sum();
        Assert.assertEquals(expectedScoreSum, score);
    }
}
