# juc-future-CompletableFuture


```text
CompletionStage链条中的CompletionStage更多扮演编排的节点.

task的完成会推动计算阶段的推进()

task1                         task2                         task3                         
------------CompletionStage █---------------CompletionStage █-------------CompletionStage █
                            |   
                            |    
                            |------------Completion(callback)
```

## AsyncSupply

```text
CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");

static <U> CompletableFuture<U> asyncSupplyStage(Executor e, Supplier<U> f) {
    if (f == null) throw new NullPointerException();
    CompletableFuture<U> d = new CompletableFuture<U>();
    e.execute(new AsyncSupply<U>(d, f));                            // 执行CompletionStage链条首节点的任务
    return d;
}
```

AsyncSupply作为CompletionStage链条的级联完成的触发器,没有/也不依赖父CompletionStage

AsyncSupply作为CompletionStage链条首节点对应的执行任务

```text
static final class AsyncSupply<T> extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
    
    Supplier<T> fn;            // 任务的具体执行逻辑
    CompletableFuture<T> dep;  // 任务归属的CompletionStage
    
    AsyncSupply(CompletableFuture<T> dep, Supplier<T> fn) {
        this.dep = dep;
        this.fn = fn;
    }
        
    public final boolean exec() { run(); return true; }

        public void run() {
            CompletableFuture<T> d; Supplier<T> f;
            if ((d = dep) != null && (f = fn) != null) {
                dep = null; fn = null;
                if (d.result == null) {
                    try {
                        d.completeValue(f.get());      // 任务的具体执行逻辑执行完,需要标记当前任务归属的计算阶段完成了
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);       // 任务执行异常也算是当前任务归属的计算阶段完成了
                    }
                }
                d.postComplete();                      // 触发那些当前计算阶段的完成回调
            }
        }
}
```

## UniApply

UniApply主要被当作某个计算阶段的completion-callback而存在,同时肩负了执行计算阶段链条的下个计算阶段的计算任务.

```text
CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> cf2 = cf1.thenApply(i -> "cf2:" + i);


private <V> CompletableFuture<V> uniApplyStage(Executor e, Function<? super T,? extends V> f) {
    if (f == null) throw new NullPointerException();
    
    CompletableFuture<V> d =  new CompletableFuture<V>();
    
    if (e != null || !d.uniApply(this, f, null)) {
        UniApply<T,V> c = new UniApply<T,V>(e, d, this, f);
        push(c);                                                          // 将UniApply作为completion-callback
        c.tryFire(SYNC);
    }
    
    return d;
}
```

```text
static final class UniApply<T,V> extends UniCompletion<T,V> {
    
    CompletableFuture<T> src;                                          // UniApply是某个计算阶段的completion-callback
    CompletableFuture<V> dep;                                          // UniApply所属的计算阶段的下个计算阶段
    Function<? super T,? extends V> fn;                                // 下个计算阶段的计算任务
    
    UniApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T,? extends V> fn) {
        super(executor, dep, src); 
        this.fn = fn;
    }
    
    final CompletableFuture<V> tryFire(int mode) {
        CompletableFuture<V> d; CompletableFuture<T> a;
        if ((d = dep) == null ||
            !d.uniApply(a = src, fn, mode > 0 ? null : this))           // 如果UniApply所属的计算阶段已经完成,则执行下个计算阶段的计算任务(并标记下个计算阶段为完成)
            return null;
        
        dep = null; src = null; fn = null;
        
        return d.postFire(a, mode);                                     // 善后工作
    }
}
```