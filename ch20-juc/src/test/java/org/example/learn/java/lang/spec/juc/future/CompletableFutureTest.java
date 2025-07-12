package org.example.learn.java.lang.spec.juc.future;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 1. 非阻塞回调（thenApply, thenAccept）。
 2. 组合多个异步操作（thenCompose, allOf）。
 3. 主动完成或异常完成（complete(), completeExceptionally()）。
 */
public class CompletableFutureTest {

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
}
