package org.example.learn.java.lang.spec.juc.future.completable;

import org.example.learn.java.lang.spec.juc.util.ThreadUtils;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)
 *
 * fn以当前计算阶段this的result为入参,fn的返回值是中间计算阶段m,m是当前计算阶段this的下游,thenCompose的返回值是m的下游n.中间计算阶段m完成后触发n完成且将m.result设置为n.result
 * fn并不是任何计算阶段的计算任务,而是当前计算阶段this的一个纯粹的completion-action. (多数的计算阶段的计算任务拥有两面性,一面是上游的completion-action,另一面是下游的计算任务)🚀🚀🚀
 * fn动态声明了用户不可见的中间计算阶段m,fn要保证m一定会被触发(通过向线程池提交异步任务获得m/通过构造已完成的m/通过thenXXX获得m)🚀🚀🚀
 */
public class ThenComposeTest {

    /**
     *  thenCompose的入参fn要保证中间计算阶段一定会被触发(通过向线程池提交异步任务获得/通过构造已完成的/通过thenXXX获得)
     */
    @Test
    public void test01() throws ExecutionException, InterruptedException {
        // 模拟异步获取用户ID
        CompletableFuture<String> userIdFuture = CompletableFuture.supplyAsync(() -> {
            ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            System.out.println("获取用户ID...");
            return "user123";
        });

        // 使用thenCompose：用用户ID异步获取用户详情
        CompletableFuture<String> userDetailFuture = userIdFuture.thenCompose(userId ->
                // 中间计算阶段一定会被触发
                CompletableFuture.supplyAsync(() -> {
                    ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
                    System.out.println("根据用户ID获取详情: " + userId);
                    return "用户详情: " + userId;
                })
        );

        System.out.println("最终结果: " + userDetailFuture.get());
    }


    @Test(expected = TimeoutException.class)
    public void test02() throws ExecutionException, InterruptedException, TimeoutException {
        // 模拟异步获取用户ID
        CompletableFuture<String> userIdFuture = CompletableFuture.supplyAsync(() -> {
            ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
            System.out.println("获取用户ID...");
            return "user123";
        });

        // 使用thenCompose：用用户ID异步获取用户详情
        CompletableFuture<String> userDetailFuture = userIdFuture.thenCompose(userId -> new CompletableFuture<>());    // 此时fn返回一个不会被触发的对象,将导致用户获得计算阶段永远不会被完成

        System.out.println("最终结果: " + userDetailFuture.get(5, TimeUnit.SECONDS));
    }
}
