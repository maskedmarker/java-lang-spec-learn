package org.example.learn.java.lang.spec.juc.pool;

import org.example.learn.java.lang.spec.juc.util.ThreadUtils;
import org.junit.Test;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorTest2 {

    private static class MyJob implements Runnable{

        @Override
        public void run() {
            System.out.println(Calendar.getInstance().getTime());
            ThreadUtils.yieldWait(3, TimeUnit.SECONDS);
        }
    }

    @Test
    public void test01() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(new MyJob());


        ThreadUtils.yieldWait(5, TimeUnit.MINUTES);
    }

    /**
     * Executors.newCachedThreadPool() 等价于new ThreadPoolExecutor(0, Integer.MAX_VALUE,60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
     *
     * SynchronousQueue.offer(e) 只有在有线程在等待时才能返回true. 如果此时线程池还没有触发shutdown的话,会新增线程来执行提交的任务
     * 使用SynchronousQueue让线程池优先创建线程而非将任务放入队列等待.
     */
    @Test
    public void test02() {
        ExecutorService executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        executorService.execute(new MyJob());


        ThreadUtils.yieldWait(5, TimeUnit.MINUTES);
    }

}
