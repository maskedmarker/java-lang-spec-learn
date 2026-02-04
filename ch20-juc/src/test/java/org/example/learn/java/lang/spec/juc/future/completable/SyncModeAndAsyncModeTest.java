package org.example.learn.java.lang.spec.juc.future.completable;

import org.example.learn.java.lang.spec.juc.util.ThreadUtils;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 同步模式和异步模式的线程模型
 *
 * 同步模式: 计算阶段的计算任务在上游的postComplete中被执行,而postComplete与上游的result被设置在同一个方法中.外层的方法在返回时postComplete和result都已完成,因此被成为同步.
 * 异步模式: 不在上游的postComplete中被执行,被提交到了线程池中.
 */
public class SyncModeAndAsyncModeTest {


    /**
     * 同步模式和异步模式的比对
     *
     */
    @Test
    public void test11(){
        try {
            Executor executor = Executors.newFixedThreadPool(5);

            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                ThreadUtils.yieldWait(2, TimeUnit.SECONDS);                            // 为了保证cf2/cf3声明时,cf还未完成
                System.out.printf("cf: in thread[%s]\n", Thread.currentThread().getName());
                return "Hello";
            }, executor);

            // 当声明同步模式的计算阶段时,cf2的计算任务是与cf的计算任务同步的关系,所以在cf的result被设置的同一个方法中被执行(即result被设置的那个语句后的postComplete中执行)
            // 当声明同步模式的计算阶段时,其计算任务的执行线程将借用上游计算任务的执行线程, 即cf/cf2的异步计算任务由同一个线程完成
            CompletableFuture<Void> cf2 = cf.thenAccept(i -> {
                System.out.printf("cf2: i = %s   in thread[%s]\n", i, Thread.currentThread().getName());
            });  // 同步模式

            // 当声明异模式的计算阶段时,其计算任务的执行被提交到线程中,不在postComplete中执行
            CompletableFuture<Void> cf3 = cf.thenAcceptAsync((i -> {
                System.out.printf("cf3: i = %s   in thread[%s]\n", i, Thread.currentThread().getName());
            }), executor); // 异步模式


            while (!cf3.isDone()) {
                ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 同步模式和异步模式的比对
     *
     */
    @Test
    public void test121(){
        try {
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                ThreadUtils.yieldWait(2, TimeUnit.SECONDS);   // 为了保证cf2/cf3声明时,cf还未完成
                System.out.printf("cf: in thread[%s]\n", Thread.currentThread().getName());
                return "Hello";
            });

            // cf的result被设置完成后,还会执行postComplete,此时执行同步模式的下游
            CompletableFuture<Void> cf2 = cf.thenAccept(i -> {
                System.out.printf("cf2: i = %s   in thread[%s]\n", i, Thread.currentThread().getName());
            });  // 同步模式


            // 当声明异模式的计算阶段时,其计算任务的执行线程会(不一定)被其他线程执行.注意这里的不一定
            // 比如这里使用默认的ForkJoinPool线程池,cf3的计算任务是异步的,所以不在cf的postComplete中执行,而是被提交到了线程池中. ForkJoinPool线程池接收到任务后,此时线程池不忙,不用新增线程,就用旧的线程执行了(而该线程可能是刚执行过cf1/cf2计算任务的线程).
            CompletableFuture<Void> cf3 = cf.thenAcceptAsync((i -> {
                System.out.printf("cf3: i = %s   in thread[%s]\n", i, Thread.currentThread().getName());
            })); // 异步模式


            while (!cf3.isDone()) {
                ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 同步模式和异步模式的比对
     *
     */
    @Test
    public void test122(){
        try {
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                ThreadUtils.yieldWait(2, TimeUnit.SECONDS);   // 为了保证cf2/cf3声明时,cf还未完成
                System.out.printf("cf: in thread[%s]\n", Thread.currentThread().getName());
                return "Hello";
            });

            CompletableFuture<Void> cf2 = cf.thenAccept(i -> {
                ThreadUtils.yieldWait(1, TimeUnit.SECONDS);                                           // 这里通过占用fork-join线程池线程,迫使cf3的计算任务在提交到线程池时由新增的线程来完成
                System.out.printf("cf2: i = %s   in thread[%s]\n", i, Thread.currentThread().getName());
            });  // 同步回调

            CompletableFuture<Void> cf3 = cf.thenAcceptAsync((i -> {
                System.out.printf("cf3: i = %s   in thread[%s]\n", i, Thread.currentThread().getName());
            })); // 异步回调


            while (!cf3.isDone()) {
                ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
