package org.example.learn.java.lang.spec.juc.future.completable;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 1. 非阻塞回调（thenApply, thenAccept）。
 2. 组合多个异步操作（thenCompose, allOf）。
 3. 主动完成或异常完成（complete(), completeExceptionally()）。
 */
public class CompletableFutureTest {

    private static class UserService {
        private final String userId;

        public UserService(String userId) {
            this.userId = userId;
        }

        CompletableFuture<String> fetchUserId() {
            return CompletableFuture.supplyAsync(() -> this.userId);
        }

        CompletableFuture<String> fetchUserProfile(String userId) {
            return CompletableFuture.supplyAsync(() -> "Profile of " + userId);
        }
    }

    /**
     * 如何获取Future对象
     */
    @Test
    public void test01(){
        try {
            // 默认使用ForkJoinPool.commonPool()
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "Hello");
            cf.thenAccept(System.out::println); // 同步回调
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test02(){
        // 使用自定义的线程池
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "Hello", executor);
            cf.thenAccept(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }


    @Test
    public void test11() throws Exception {
        UserService userService = new UserService("user001");
        CompletableFuture<String> profileFuture = userService.fetchUserId().thenCompose(userService::fetchUserProfile);
        System.out.println("profileFuture.get() = " + profileFuture.get());
    }
}
