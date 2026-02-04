# juc-future

Future是Java 5引入的接口,用于表示一个异步计算的结果(提供了统一的接口).它提供了检查计算是否完成、等待计算完成、以及获取计算结果的机制.


## javadoc

```text
A Future represents the result of an asynchronous computation. 
Methods are provided to check if the computation is complete, to wait for its completion, and to retrieve the result of the computation. 
The result can only(💯) be retrieved using method get when the computation has completed, blocking if necessary until it is ready. 
Cancellation is performed by the cancel method. Additional methods are provided to determine if the task completed normally or was cancelled. 
Once a computation has completed, the computation cannot be cancelled. 
If you would like to use a Future for the sake of cancellability but not provide a usable result, you can declare types of the form Future<?> and return null as a result of the underlying task.
```


```text
public interface Future<V> {

    // 取消任务,mayInterruptIfRunning 表示是否允许中断正在运行的任务
    boolean cancel(boolean mayInterruptIfRunning);

    // 任务是否已取消
    boolean isCancelled();

    // 任务是否已完成(正常完成/异常) 计算包含正常和计算中抛出异常. 
    boolean isDone();

    // 阻塞获取结果(当前线程已经或者等待中被中断,本线程抛出InterruptedException, 计算过程发生异常则抛出ExecutionException)
    V get() throws InterruptedException, ExecutionException;

    // 带超时的阻塞获取结果
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
```


## Future 的局限性
虽然 Future 提供了异步能力,但存在以下限制：
1. 无法手动完成：任务一旦提交,无法手动设置结果(除非通过复杂手段). 
2. 阻塞问题：get() 方法会阻塞,可能导致性能问题. 
3. 链式调用困难：无法直接对结果进行后续操作(如回调、组合). 
4. 异常处理繁琐：需要捕获 ExecutionException 并解析原始异常.


## 现代替代方案：CompletableFuture
Java 8 引入的 CompletableFuture 是对 Future 的增强,支持：
1. 非阻塞回调(thenApply, thenAccept). 
2. 组合多个异步操作(thenCompose, allOf). 
3. 主动完成或异常完成(complete(), completeExceptionally()).

总结

特性     	    Future	       CompletableFuture
异步计算	        ✅ 支持	       ✅ 支持
阻塞获取结果	    ✅ get()	   ✅ get()(不推荐)
非阻塞回调	    ❌ 不支持	   ✅ thenApply 等
组合多个任务	    ❌ 复杂	       ✅ thenCompose
手动完成/异常完成	❌ 不支持	   ✅ complete()


