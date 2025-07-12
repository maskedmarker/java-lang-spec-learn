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

```