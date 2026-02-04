package org.example.learn.java.lang.spec.juc.future;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Future 是 Java 5 引入的接口,用于表示一个异步计算的结果
 * 通常通过 ExecutorService 提交任务（Callable 或 Runnable）来获取 Future 实例
 */
public class FutureTest {

    /**
     * 如何获取Future对象
     */
    @Test
    public void test01(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 提交一个 Callable 任务,返回 Future
            Future<Integer> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
                return 42;
            });
            System.out.println("异步任务已提交,主线程继续执行...");

            // 获取结果（如果任务未完成会阻塞）
            Integer result = future.get();
            System.out.println("异步任务计算结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 如何获取Future对象
     */
    @Test
    public void test02(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 提交一个 Callable 任务,返回 Future
            Future<Integer> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
                return 42;
            });
            System.out.println("异步任务已提交,主线程继续执行...");

            // 检查任务是否完成
            while (!future.isDone()) {
                System.out.println("异步任务未完成,当前线程去做其他任务...");
                TimeUnit.MILLISECONDS.sleep(500);
            }

            // 通过前面检查future.isDone(),避免获取结果被阻塞
            Integer result = future.get();
            System.out.println("异步任务计算结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 如何获取Future对象
     */
    @Test
    public void test03(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 提交一个 Callable 任务,返回 Future
            Future<Integer> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
                throw new RuntimeException("异步任务发生异常");
            });
            System.out.println("异步任务已提交,主线程继续执行...");

            // 检查任务是否完成
            while (!future.isDone()) {
                System.out.println("异步任务未完成,当前线程去做其他任务...");
                TimeUnit.MILLISECONDS.sleep(500);
            }

            System.out.println("如果异步任务抛出异常,调用future.get()将收获一个new ExecutionException,ExecutionException的cause就是异步计算时抛出的那个异常");
            Integer result = future.get();
            System.out.println("异步任务计算结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 取消异步计算的任务
     */
    @Test
    public void test11(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 提交一个 Callable 任务,返回 Future
            Future<Integer> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
                return 42;
            });
            System.out.println("异步任务已提交,主线程继续执行...");

            // 检查异步任务是否完成
            int maxWaitCount = 2;
            int waitCount = 0;
            while (!future.isDone()) {
                if (waitCount >= maxWaitCount) {
                    System.out.println("开始取消异步任务");
                    // This attempt will fail if the task has already completed, has already been cancelled, or could not be cancelled for some other reason.
                    // cancel()的返回值仅仅是本次尝试取消任务是否完成,并非指的是异步计算任务本身是否已经取消(比如存在异步计算任务已经被其他线程取消,那么本次取消尝试肯定是失败的).
                    boolean cancelled = future.cancel(true);

                    // cancel()的返回值与isCancelled()并不总是相同的,当前简单的单线程模拟环境肯定是相同的
                    assert cancelled == future.isCancelled();
                    System.out.println("异步任务已取消: " + future.isCancelled());
                    break;
                }

                System.out.println("异步任务未完成,当前线程去做其他任务...");
                TimeUnit.MILLISECONDS.sleep(500);
                waitCount++;
            }

            System.out.println("如果异步计算任务已经取消,就会抛出异常CancellationException");
            Integer result = future.get();
            System.out.println("异步任务计算结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 获取异步计算的任务的结果,可能会抛出异常
     * Future.get()的阻塞等待过程可以通过中断结束.抛出异常可以让调用方知晓是因为中断跳出等待而非任务结束导致的跳出等待
     *
     * 在不增加额外属性标识的情况下,如何区分是因为阻塞等待正常结束还未被中断结束? java选择通过中断异常来区分.
     */
    @Test
    public void test21(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 提交一个 Callable 任务,返回 Future
            Future<Integer> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
                return 42;
            });
            System.out.println("异步任务已提交,主线程继续执行...");

            System.out.println("模拟主线程被其他线程interrupt");
            Thread.currentThread().interrupt();

            System.out.println("在任务还未完成的情况下,get()方法的调用方会等待异步任务完成.如果此时调用方处于interrupted状态,调用get()会立即抛出中断异常跳出阻塞过程");
            Integer result = future.get();
            System.out.println("异步计算结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 获取异步计算的任务的结果,可能会抛出异常
     *
     * 在不增加额外属性标识的情况下,如何区分是因为阻塞等待正常结束还未被中断结束? java选择通过中断异常来区分.
     */
    @Test
    public void test22(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 提交一个 Callable 任务,返回 Future
            Future<Integer> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
                return 42;
            });
            System.out.println("异步任务已提交,主线程继续执行...");


            // 检查任务是否完成
            while (!future.isDone()) {
                System.out.println("等待异步任务完成...");
                TimeUnit.MILLISECONDS.sleep(500);
            }
            System.out.println("异步任务已完成");


            System.out.println("模拟主线程被其他线程interrupt");
            Thread.currentThread().interrupt();

            System.out.println("异步任务已经完成的情况下,且get()方法的调用方处于interrupted状态,因为调用get()方法不会发生阻塞等待,所以可以保留中断状态位");
            Integer result = future.get();
            System.out.println("异步计算结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 获取异步计算的任务的结果,可能会抛出异常
     */
    @Test
    public void test23(){
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 提交一个 Callable 任务,返回 Future
            Future<Integer> future = executor.submit(() -> {
                TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
                throw new RuntimeException("异步任务发生异常");
            });
            System.out.println("异步任务已提交,主线程继续执行...");


            // 检查任务是否完成
            while (!future.isDone()) {
                System.out.println("等待异步任务完成...");
                TimeUnit.MILLISECONDS.sleep(500);
            }
            System.out.println("异步任务已完成");


            System.out.println("模拟主线程被其他线程interrupt");
            Thread.currentThread().interrupt();

            System.out.println("当前线程被interrupted的状态下,如果异步任务已经完成,获取结果不会被阻塞且不会抛出中断异常");
            Integer result = future.get();
            System.out.println("异步计算结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
