package org.example.learn.java.lang.spec.thread;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ThreadJoinTest {

    /**
     * Thread已经notAlive后,再join时,会立即返回
     */
    @Test
    public void test0() throws InterruptedException {
        Thread workerThread = new Thread("worker-thread");
        workerThread.start();
        System.out.printf("创建线程thread[%s],并start该thread\n", workerThread.getName());

        // 等待workerThread结束
        while (workerThread.isAlive()) {
            Thread.yield();
        }

        System.out.printf("thread[%s] starts to join thread[%s] at %s\n", Thread.currentThread(), workerThread.getName(), System.currentTimeMillis());
        long start = System.currentTimeMillis();
        workerThread.join();
        System.out.printf("thread[%s] finish to join thread[%s] at %s\n", Thread.currentThread(), workerThread.getName(), System.currentTimeMillis());
        Assert.assertTrue("如果被join的线程已经un-alive,当前线程会直接退出join方法", ((System.currentTimeMillis() - start) < TimeUnit.MILLISECONDS.toMillis(100)));
    }
}
