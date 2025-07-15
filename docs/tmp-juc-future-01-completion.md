# juc-future

## CompletableFuture
CompletableFuture不是抽象类也不是接口.
属性result用来存储该异步计算的结果值
    异常用AltResult包装后再赋值.
    null值也用AltResult包装后再赋值.(因为CompletableFuture是通过result是否等于null用来表示该Future的计算是否已完成)
属性stack用来存储该异步计算完成后才会触发的后续操作.
    这些后续操作通过数据结构stack保存

```text
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    volatile Object result;       // Either the result or boxed AltResult
    volatile Completion stack;    // Top of Treiber stack of dependent actions
}
```


## UniCompletion



Completion定义了CompletableFuture.stack的栈元素
```text
abstract static class Completion extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
    
    volatile Completion next;   // 通过next实现数据结构栈(Treiber stack)
}
```

UniCompletion
```text
abstract static class UniCompletion<T,V> extends Completion {

    Executor executor;                 // executor to use (null if none)
    CompletableFuture<V> dep;          // the dependent to complete
    CompletableFuture<T> src;          // source for action
}
```