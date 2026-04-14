package org.example.learn.java.lang.spec.juc.pool;

import org.junit.Test;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 同一个任务不会被并发执行
 * 任务执行时间大于周期时,下一次任务会在前一次任务结束后立刻开始，整体执行频率低于周期
 */
public class ScheduledThreadPoolExecutorTest {

    private static class WarmupJob implements Runnable{

        @Override
        public void run() {
            System.out.println(Calendar.getInstance().getTime() + Thread.currentThread().getName() + " begin");
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(Calendar.getInstance().getTime() + Thread.currentThread().getName() + " end");
        }
    }

    private static class MyJob implements Runnable{

        @Override
        public void run() {
            System.out.println(Calendar.getInstance().getTime() + Thread.currentThread().getName() + " begin");
            try {
                TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(3,6));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(Calendar.getInstance().getTime() + Thread.currentThread().getName() + " end");
        }
    }

    private static class MyJob2 implements Runnable{

        @Override
        public void run() {
            System.out.println(Calendar.getInstance().getTime() + Thread.currentThread().getName() + " begin");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(Calendar.getInstance().getTime() + Thread.currentThread().getName() + " end");
        }
    }

    @Test
    public void test01() throws InterruptedException {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.submit(new WarmupJob());
        scheduledExecutorService.submit(new WarmupJob());
        scheduledExecutorService.submit(new WarmupJob());
        scheduledExecutorService.scheduleAtFixedRate(new MyJob(), 0, 2, TimeUnit.SECONDS);   // 任务开始的时间是固定的离散点,下一次开始时间 = 上一次开始时间 + period


        while (!scheduledExecutorService.isShutdown()) {
            Thread.yield();
            TimeUnit.SECONDS.sleep(1);
        }
    }

    /**
     */
    @Test
    public void test02() throws InterruptedException {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
        scheduledExecutorService.scheduleWithFixedDelay(new MyJob2(), 0, 2, TimeUnit.SECONDS);  // 任务开始的时间不是固定的离散点 下一次开始时间 = 上一次结束时间 + delay


        while (!scheduledExecutorService.isShutdown()) {
            Thread.yield();
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void test03() throws InterruptedException {
        ScheduledExecutorService pool = new ScheduledThreadPoolExecutor(2);

        pool.scheduleAtFixedRate(() -> {
            System.out.println(Thread.currentThread().getName() + " start");
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(Thread.currentThread().getName() + " end");
        }, 0, 5, TimeUnit.SECONDS);


        while (!pool.isShutdown()) {
            Thread.yield();
            TimeUnit.SECONDS.sleep(1);
        }
    }

}
