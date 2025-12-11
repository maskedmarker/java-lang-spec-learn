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

    private int score = 0;

    @Test
    public void test0() throws InterruptedException {
        Object obj = new Object();

        Thread[] workerThreads = new Thread[100];
        for (int i = 0; i < workerThreads.length; i++) {
            final int newScore = i;
            Thread workerThread = new Thread(() -> {
                addScore(obj, newScore);
            });
            workerThread.start();

            workerThreads[i] = workerThread;
        }

        for (Thread workerThread : workerThreads) {
            workerThread.join();
        }
        int expectedScoreSum = IntStream.rangeClosed(0, 100-1).sum();
        Assert.assertEquals(expectedScoreSum, score);
    }

    /**
     * 为了解释方便,假设抢占锁时,使用公平机制,同时忽略重入机制
     *
     *  每个线程先去同步队列排队等待,当成为队首的线程就会结束等待获得锁,使用完后,退出队列(也就释放了锁);
     *  队首后的下一个线程成为新的队首,然后就会获得锁结束等待,使用完后,退出队列(也就释放了锁);
     *  and so on;
     */
    void addScore(Object obj, int newValue) {
        synchronized (obj) {        // 等待成为同步队列队首(然后就会获取obj的monitor锁)
            score += newValue;      // 执行临界区的代码 (临界区指的是会发生竟态问题的代码区域)
        }                           // 退出队列(也就释放了锁)
    }
}
