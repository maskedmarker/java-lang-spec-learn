# juc-future

## 什么是Future
Future 是 Java 5 引入的接口，用于表示一个异步计算的结果。它提供了检查计算是否完成、等待计算完成、以及获取计算结果的机制。

```text
public interface Future<V> {

    // 取消任务，mayInterruptIfRunning 表示是否允许中断正在运行的任务
    boolean cancel(boolean mayInterruptIfRunning);

    // 任务是否已取消
    boolean isCancelled();

    // 任务是否已完成（正常完成、异常或取消）
    boolean isDone();

    // 阻塞获取结果（会抛出 InterruptedException, ExecutionException）
    V get() throws InterruptedException, ExecutionException;

    // 带超时的阻塞获取结果
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
```

## 如何使用Future

通常通过 ExecutorService 提交任务（Callable 或 Runnable）来获取 Future 实例。
```text
// 提交一个 Callable 任务，返回 Future
Future<Integer> future = executor.submit(() -> {
    TimeUnit.SECONDS.sleep(2); // 模拟耗时计算
    return 42;
});
```

## Future 的局限性
虽然 Future 提供了异步能力，但存在以下限制：
1. 无法手动完成：任务一旦提交，无法手动设置结果（除非通过复杂手段）。 
2. 阻塞问题：get() 方法会阻塞，可能导致性能问题。 
3. 链式调用困难：无法直接对结果进行后续操作（如回调、组合）。 
4. 异常处理繁琐：需要捕获 ExecutionException 并解析原始异常。


## 现代替代方案：CompletableFuture
Java 8 引入的 CompletableFuture 是对 Future 的增强，支持：
1. 非阻塞回调（thenApply, thenAccept）。 
2. 组合多个异步操作（thenCompose, allOf）。 
3. 主动完成或异常完成（complete(), completeExceptionally()）。

总结

特性     	    Future	       CompletableFuture
异步计算	        ✅ 支持	       ✅ 支持
阻塞获取结果	    ✅ get()	   ✅ get()（不推荐）
非阻塞回调	    ❌ 不支持	   ✅ thenApply 等
组合多个任务	    ❌ 复杂	       ✅ thenCompose
手动完成/异常完成	❌ 不支持	   ✅ complete()


