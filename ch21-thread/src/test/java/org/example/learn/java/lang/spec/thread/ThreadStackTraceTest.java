package org.example.learn.java.lang.spec.thread;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 获取线程当前的调用栈
 */

public class ThreadStackTraceTest {

    @Test
    public void test00() {
        Thread workerThread = new Thread("worker-thread");
        System.out.printf("创建线程thread[%s],但是不start thread", workerThread.getName());

        StackTraceElement[] stackTraceElements = workerThread.getStackTrace();
        Assert.assertTrue("没有start的线程,它的调用栈是空的", stackTraceElements.length == 0);


        workerThread.start();
        // 等待workerThread结束
        while (workerThread.isAlive()) {
            Thread.yield();
        }
        StackTraceElement[] stackTraceElements2 = workerThread.getStackTrace();
        Assert.assertTrue("已经结束的线程,它的调用栈也是空的", stackTraceElements2.length == 0);
    }

    @Test
    public void test01() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Assert.assertTrue("已经start的线程,它的调用栈不是空的", stackTraceElements.length != 0);
        Arrays.stream(stackTraceElements).forEach(System.out::println);
    }

    @Test
    public void test02() {
        for (int i = 0; i < 100; i++) {
            doTest02();
        }
    }

    private void doTest02() {
        // 调用当前thread的getStackTrace()方法,底层调用的是(new Exception()).getStackTrace()方法
        StackTraceElement[] stackTraceElements1 = Thread.currentThread().getStackTrace();
        Assert.assertTrue("已经start的线程,它的调用栈不是空的", stackTraceElements1.length != 0);
        Arrays.stream(stackTraceElements1).forEach(System.out::println);
        System.out.println("------------------------------------------------");

        Thread workerThread = new Thread(() -> {
            while (true) {
                Thread.yield();
            }
        }, "worker-thread");
        workerThread.start();

        // 等待workerThread开始运行
        while (!workerThread.isAlive()) {
            Thread.yield();
        }
        System.out.printf("thread[%s] isAlive = %s\n", workerThread.getName(), workerThread.isAlive());

        // 由于isAlive(): Tests if this thread is alive. A thread is alive if it has been started and has not yet died.
        // 可能存在线程已经start,还未来得及执行第一个方法,此时也是alive,但是调用栈为空,所以需要等一会
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        // 调用非当前thread的getStackTrace()方法,底层调用的是Thread.dumpThreads()方法
        StackTraceElement[] stackTraceElements2 = workerThread.getStackTrace();
        Assert.assertTrue("已经start的线程,它的调用栈不是空的", stackTraceElements2.length != 0);
        Arrays.stream(stackTraceElements1).forEach(System.out::println);
    }

}
