package org.example.learn.java.lang.spec.juc.pool;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Time;
import java.util.Calendar;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolExecutorTest {

    private static class MyTask implements Runnable {

        private static final AtomicInteger index = new AtomicInteger(1);
        final private String name;
        private TimeUnit unit;
        long workingTime;
        private volatile boolean done;

        public MyTask(String namePrefix, long workingTime, TimeUnit unit) {
            this.name = namePrefix + (index.getAndIncrement());
            this.unit = unit;
            this.workingTime = workingTime;
        }

        @Override
        public void run() {
            System.out.printf("task[%s] starts at %s by thread[%s](interrupted=%s) \n", name, Calendar.getInstance().getTime(), Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
            long start = System.currentTimeMillis();
            long end = start + unit.toMillis(workingTime);
            while (true) {
                if (System.currentTimeMillis() >= end) {
                    break;
                }

                // 模拟任务耗时
                try {
                    this.unit.sleep(workingTime);
                } catch (InterruptedException ignore) {
                }
            }

            System.out.printf("task[%s] finish at %s by thread[%s](interrupted=%s) \n", name, Calendar.getInstance().getTime(), Thread.currentThread().getName(), Thread.currentThread().isInterrupted());
            done = true;
        }

        public boolean isDone() {
            return done;
        }
    }


    @Test
    public void test01() {
        int corePoolSize = 1;
        int maximumPoolSize = 3;
        long keepAliveTime = 5;
        BlockingDeque<Runnable> workQueue = new LinkedBlockingDeque(2);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);

        // execute没有返回值
        poolExecutor.execute(new MyTask("task", 5, TimeUnit.SECONDS));
        // submit会返回future
        Future<?> task2Future = poolExecutor.submit(new MyTask("task", 10, TimeUnit.SECONDS));

        while (poolExecutor.getCompletedTaskCount() < 2) {
            Thread.yield();
        }

        /*
        // 也可以使用Future.isDone
        while (!task2Future.isDone()) {
            Thread.yield();
        }*/
    }

    /**
     * 优先创建所谓核心线程来处理任务
     * 核心线程创建满了,放入任务队列
     * 任务队列满了,创建临时线程来应对.
     */
    @Test
    public void test02() {
        int corePoolSize = 1;
        int maximumPoolSize = 1+2;
        long keepAliveTime = 5;
        int maxWorkQueueSize = 2;
        BlockingDeque<Runnable> workQueue = new LinkedBlockingDeque(maxWorkQueueSize);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);
        // 可以观测workQueue的瞬时大小
        BlockingQueue<Runnable> poolExecutorWorkingQueue = poolExecutor.getQueue();


        // 优先创建所谓核心线程来处理任务
        poolExecutor.execute(new MyTask("task", 10, TimeUnit.SECONDS));
        Assert.assertEquals("优先创建所谓核心线程来处理任务", 1, poolExecutor.getActiveCount());
        Assert.assertEquals("优先创建所谓核心线程来处理任务.核心线程创建满了,才放入任务队列", 0, poolExecutorWorkingQueue.size());



        // 核心线程创建满了,放入任务队列
        for (int i = 0; i < maxWorkQueueSize; i++) {
            poolExecutor.execute(new MyTask("task", 10, TimeUnit.SECONDS));
            Assert.assertEquals("核心线程创建满了,放入任务队列", (i + 1), poolExecutorWorkingQueue.size());
            Assert.assertEquals("核心线程创建满了,放入任务队列", 1, poolExecutor.getActiveCount());
        }
        Assert.assertEquals("任务队列满之前,不会新增工作线程", 1, poolExecutor.getActiveCount());



        // 任务队列满了,创建临时线程来应对
        for (int i = 0; i < (maximumPoolSize - corePoolSize); i++) {
            poolExecutor.execute(new MyTask("task", 10, TimeUnit.SECONDS));
            Assert.assertEquals("任务队列满了,创建临时线程来应对", maxWorkQueueSize, poolExecutorWorkingQueue.size());
            Assert.assertEquals("任务队列满了,创建临时线程来应对", (1 + (i + 1)), poolExecutor.getActiveCount());
        }

        // 核心线程满了,任务队列满了,临时线程数也满了
        Assert.assertEquals("任务队列满了", maxWorkQueueSize, poolExecutorWorkingQueue.size());
        Assert.assertEquals("核心线程满了, 临时线程数也满了", maximumPoolSize, poolExecutor.getActiveCount());
        try {
            poolExecutor.execute(new MyTask("task", 10, TimeUnit.SECONDS));
        } catch (RejectedExecutionException e) {
            Assert.assertEquals("核心线程满了,任务队列满了,临时线程数也满了,再提交任务会被拒绝", maximumPoolSize, poolExecutor.getActiveCount());
        }
    }

    @Test
    public void test03() {
        int corePoolSize = 1;
        int maximumPoolSize = 3;
        long keepAliveTime = 5;
        BlockingDeque<Runnable> workQueue = new LinkedBlockingDeque(2);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);

        // execute没有返回值
        poolExecutor.execute(new MyTask("task", 5, TimeUnit.SECONDS));
        // submit会返回future
        Future<?> task2Future = poolExecutor.submit(new MyTask("task", 10, TimeUnit.SECONDS));

        // 可以观测workQueue的瞬时大小
        BlockingQueue<Runnable> blockingQueue = poolExecutor.getQueue();
        boolean firstDetect = true;
        int workQueueSize = blockingQueue.size();
        while (poolExecutor.getCompletedTaskCount() < 2) {
            if (firstDetect || (blockingQueue.size() != workQueueSize)) {
                firstDetect = false;
                workQueueSize = blockingQueue.size();
                System.out.println("workQueueSize = " + workQueueSize);
            }
            Thread.yield();
        }
    }
}
