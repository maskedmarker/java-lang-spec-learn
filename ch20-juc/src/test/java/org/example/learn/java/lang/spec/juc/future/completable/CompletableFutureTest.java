package org.example.learn.java.lang.spec.juc.future.completable;

import org.example.learn.java.lang.spec.juc.util.ThreadUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class CompletableFutureTest {

    /**
     * 如何获取FCompletableFuture对象
     *
     *  默认使用ForkJoinPool.commonPool()线程池
     */
    @Test
    public void test01(){
        try {
            // 通过静态方法,提交一个异步任务,获得一个增强版Future(CompletableFuture),异步任务由线程池而非当前线程执行完成
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "Hello");   // 有output,主要利用Supplier的运行结果(即output)
            CompletableFuture<Void> cf2 = CompletableFuture.runAsync(() -> System.out.println("hello")); // 无output,主要利用Runnable的运行side-effect


            // 手动实例化一个已完成的增强版Future
            CompletableFuture<String> cf3 = CompletableFuture.completedFuture("done3"); // 有output,入参即output


            // 手动实例化一个未完成的,并手动正常完成
            CompletableFuture<String> cf4 = new CompletableFuture<>();
            cf3.complete("done4");
            // 手动实例化一个未完成的,并手动异常完成
            CompletableFuture<String> cf5 = new CompletableFuture<>();
            cf3.completeExceptionally(new RuntimeException("done5"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 如何获取FCompletableFuture对象
     *
     *  使用自定义线程池
     */
    @Test
    public void test02(){
        try {
            // 使用自定义线程池
            Executor executor = Executors.newFixedThreadPool(5);
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                ThreadUtils.yieldWait(2, TimeUnit.SECONDS);
                return "Hello";
            }, executor);


            // 这里是为了演示,采用忙等待. 实际开发中可以使用join/get来实现阻塞等待
            while (!cf.isDone()) {
                ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            }
            System.out.println("cf is done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // region 编排 ------------------------------------------------------------------------------------------------------------------

    /**
     * 编排 FCompletableFuture
     *    如下都是计算阶段正常完成才会触发下游的计算任务
     */
    @Test
    public void test11(){
        try {
            CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");


            // 计算任务类型为Function,function的入参是上游的result,出参是计算任务所属的计算阶段的result
            CompletableFuture<String> cf2 = cf1.thenApply(i -> "cf2:" + i);
            CompletableFuture<String> cf3 = cf1.thenApply(i -> "cf3:" + i);


            CompletableFuture<Void> cf41 = cf2.runAfterBoth(cf3, () -> System.out.println("cf41: cf2 and cf3 is done"));  // 计算阶段的计算任务无需input也无output(本计算阶段无result)
            CompletableFuture<Void> cf42 = cf2.thenAcceptBoth(cf3, (i, j) -> System.out.println("cf42: " + i + j));       // 计算阶段的计算任务需要2个上游的result为input,但无output(本计算阶段无result)
            CompletableFuture<String> cf5 = cf2.thenCombine(cf3, (i, j) -> i + j);                                        // 计算阶段的计算任务需要2个上游的result为input,且有output(即本计算阶段的result)
            String cf5Result = cf5.get();    // 直到cf5完成才返回,在此之前会阻塞当前线程
            System.out.println("cf5Result = " + cf5Result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 编排 FCompletableFuture
     *    如下都是计算阶段正常/非正常完成都会触发下游的计算任务
     */
    @Test
    public void test121(){
        try {
            CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");

            CompletableFuture<String> cf2 = cf1.whenComplete((i, e) -> System.out.println("cf2: " + (e == null ? i : e)));   // 计算阶段的计算任务需要上游的result(正常值和异常)为input,但忽视计算任务的output,采用上游的result为本计算阶段的result 💯💯
            Assert.assertEquals("whenComplete: 采用上游的result为本计算阶段的result", cf1.get(), cf2.get()); // 直到完成才返回,在此之前会阻塞当前线程

            CompletableFuture<String> cf3 = cf1.handle((i, e) -> "cf3:" + (e == null ? i : e));                              // 计算阶段的计算任务需要上游的result(正常值和异常)为input,且output为本计算阶段的result(本计算阶段的计算任务发生异常也result)
            System.out.println("cf3.join() = " + cf3.join());  // 直到cf3完成才返回,在此之前会阻塞当前线程
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 编排 FCompletableFuture
     *    whenComplete(BiConsumer)
     *          BiConsumer的accept方法无返回值.
     *          下游计算任务正常执行,完全复用上游的result;
     *          下游计算任务执行异常,上游也异常,优先复用上游的异常(设计者假设了:认为是上游的异常导致下游的计算异常)
     *          下游计算任务执行异常,上游正常,使用下游计算任务的异常
     */
    @Test
    public void test122(){
        try {
            CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
                int p = ThreadLocalRandom.current().nextInt(0, 3);
                if (p > 0) {
                    throw new RuntimeException("cf1发生异常");
                }
                return "Hello";
            });
            CompletableFuture<String> cf2 = cf1.whenCompleteAsync((i, e) -> {
                if (e != null) {
                    System.out.println("cf2: 上游有异常." + e);
                }

                int p = ThreadLocalRandom.current().nextInt(0, 3);
                if (p > 0) {
                    throw new RuntimeException("cf2发生异常");
                }
            });

            try {
                cf2.get();
            } catch (InterruptedException e) {
                throw e;
            } catch (ExecutionException e) {
                Assert.assertTrue("需要从ExecutionException.getCause获取到用户代码抛出的异常", e.getCause() instanceof RuntimeException);
                Assert.assertEquals("需要从ExecutionException.getCause获取到用户代码抛出的异常", "cf1发生异常", e.getCause().getMessage());

                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test123(){
        try {
            CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
                int p = ThreadLocalRandom.current().nextInt(-1, 0);
                if (p > 0) {
                    throw new RuntimeException("cf1发生异常");
                }
                return "Hello";
            });
            CompletableFuture<String> cf2 = cf1.whenCompleteAsync((i, e) -> {
                if (e != null) {
                    System.out.println("cf2: 上游有异常." + e);
                }

                int p = ThreadLocalRandom.current().nextInt(0, 3);
                if (p > 0) {
                    throw new RuntimeException("cf2发生异常");
                }
            });

            try {
                cf2.get();
            } catch (InterruptedException e) {
                throw e;
            } catch (ExecutionException e) {
                Assert.assertTrue("需要从ExecutionException.getCause获取到用户代码抛出的异常", e.getCause() instanceof RuntimeException);
                Assert.assertEquals("需要从ExecutionException.getCause获取到用户代码抛出的异常", "cf2发生异常", e.getCause().getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
