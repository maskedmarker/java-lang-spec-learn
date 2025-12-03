# juc-future

## ForkJoinTask

```text
public abstract class ForkJoinTask<V> implements Future<V>, Serializable {

    // These control bits occupy only (some of) the upper half (16 bits) of status field. The lower bits are used for user-defined tags. 
    volatile int status; // The run status of this task
    
    public final boolean compareAndSetForkJoinTaskTag(short e, short tag) {
        for (int s;;) {
            if ((short)(s = status) != e) // 通过short强转来实现只取低16位
                return false;
            if (U.compareAndSwapInt(this, STATUS, s, (s & ~SMASK) | (tag & SMASK)))
                return true;
        }
    }
}

status的备注:
The status field holds run control status bits packed into a single int to minimize footprint and to ensure atomicity (via CAS). 
Status is initially zero, and takes on nonnegative values until completed, upon which status (anded with DONE_MASK) holds value NORMAL, CANCELLED, or EXCEPTIONAL.
Tasks undergoing blocking waits by other threads have the SIGNAL bit set.  Completion of a stolen task with SIGNAL set awakens any waiters via notifyAll.
These control bits occupy only (some of) the upper half (16 bits) of status field. The lower bits are used for user-defined tags. 
```

## CompletableFuture
CompletableFuture不是抽象类也不是接口.
属性result用来存储该异步计算的结果值
异常用AltResult包装后再赋值.
null值也用AltResult包装后再赋值.(因为CompletableFuture是通过result是否等于null用来表示该Future的计算是否已完成)
属性stack用来存储该异步计算完成后才会触发的后续操作.
这些后续操作通过数据结构stack保存

```text
public interface CompletionStage<T> {
    public <U> CompletionStage<U> thenApply(Function<? super T,? extends U> fn);
    public CompletionStage<Void> thenAccept(Consumer<? super T> action);
    public CompletionStage<Void> thenRun(Runnable action);
    public <U,V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn);
    public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);
}

Function的方法使用的动词是apply,Consumer的方法使用的动词是accept,Runnable的方法使用的动词是run,所以CompletableFuture的thenXXX与之相对应.

假定我们将当前的this CompletionStage称为 s1, 其方法返回的CompletionStage称为s2
thenApply/thenAccept/thenRun中 s2的值依赖实时计算fn的结果,而fn的入参又依赖异步计算s1的值,所以s2的完成主要依赖s1异步计算的完成
thenCombine中,other称为s3, s2的值依赖实时计算fn的结果,而fn的入参又依赖异步计算s1和s3的值,所以s2的完成主要依赖s1和s3异步计算的完成
thenCompose中,s2的值依赖由实时计算fn产生的异步计算,而实时计算fn的入参由依赖异步计算s1的值,所以s2的完成不仅依赖s1异步计算的完成,还依赖fn产生的异步计算的完成. 注意:为了flattening,s2的值等于n产生的异步计算的值或者异常

------------------------------------------------------------------
thenCompose is like “flatMap” for futures
Assume you have:
    CompletionStage<T> stage1;
and you call:
    CompletionStage<U> stage2 = stage1.thenCompose(fn);

Here’s what happens:
1. When stage1 completes successfully with a value t,the function fn.apply(t) is called.
2. fn.apply(t) returns another CompletionStage — say innerStage.
3. thenCompose flattens this into a single CompletionStage<U>,so that when innerStage completes, stage2 completes with the same result.
4. If stage1 or innerStage fails, the resulting stage completes exceptionally.

This flattening avoids nested stages like CompletionStage<CompletionStage<U>>.  

Let’s say you have two asynchronous methods:
    CompletableFuture<User> getUserAsync(int id);
    CompletableFuture<Address> getAddressAsync(User user);      
If you write:
    CompletableFuture<CompletableFuture<Address>> nested = getUserAsync(1).thenApply(user -> getAddressAsync(user));    
You get a nested future (CompletableFuture<CompletableFuture<Address>>) — not what you want.

But if you use thenCompose:  
    CompletableFuture<Address> flat =  getUserAsync(1).thenCompose(user -> getAddressAsync(user));  
You get a flattened future:
    CompletableFuture<Address> that completes only when getAddressAsync() completes.
```

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