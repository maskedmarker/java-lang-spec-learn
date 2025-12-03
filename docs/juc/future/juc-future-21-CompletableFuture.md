# juc-future-2


## CompletableFuture

FutureTask 是在 Java 5 (JDK 1.5) 中引入的,它是随着 java.util.concurrent 包一起发布的.
CompletableFuture 是 Java 8 引入的一个强大的异步编程工具,它实现了 Future 接口,并提供了丰富的 API 来支持异步任务的组合、链式调用、异常处理等功能.
相比传统的 Future,CompletableFuture 更加灵活,适合构建复杂的异步流程.

```text
“Completable”这个命名字面意思是“可完成的”,但这在并发编程语境下有特定技术含义.
核心在于与传统Future的对比——传统Future的结果只能被动等待计算完成,而CompletableFuture允许开发者主动干预这个“完成”过程.

Doug Lea在设计时特别强调了这个特性.需要重点说明三个维度：手动完成能力（complete方法）、外部完成场景（网络回调、超时处理）、组合式完成的编程模型.
外部完成场景-网络回调指的是啥?
外部完成场景-超时处理指的是超时也会触发该CompletableFuture状态反转为done
组合式完成指的是组合中被依赖CompletableFuture的的完成（无论是正常还是异常）会导致依赖CompletableFuture自动触发下一个阶段的执行.

⚠️ 注意:
用户可能特别关心组合式完成,因为这是CompletableFuture最强大的特性.
为什么这个特性如此重要？
因为它解耦了任务触发和结果处理,允许用声明式构建异步流水线.


需要强调complete()/completeExceptionally()这两个关键方法,以及它们如何实现完成传播链——这正是“组合式异步编程”的基础.
```

CompletableFuture 名称中的 Completable 是其最核心、最区别于传统 Future 接口的关键特性.
```text
它的含义主要体现在以下几个方面：
1. 手动完成能力（Manual Completion）：
    开发者可以主动地、手动地设置 CompletableFuture 的结果或异常状态,而不仅仅被动地等待后台计算线程的完成.
    核心方法：
        complete(T value): 手动将 Future 设置为完成状态,并将结果设置为 value.如果 Future 尚未完成,则返回 true；如果已经完成（无论是正常完成、取消还是手动完成过）,则返回 false.
        completeExceptionally(Throwable ex): 手动将 Future 设置为异常完成状态,并将异常设置为 ex.同样,只有 Future 未完成时才生效.
    意义： 
        这打破了传统 Future 只能由执行线程设置结果的限制.结果可以来源于：
            后台计算线程（传统方式）.
            任何其他线程（例如,响应一个网络回调、一个定时器事件、用户输入、另一个异步操作的结果）.
            甚至不需要一个具体的执行线程（例如,基于缓存直接提供结果）.   

2. 可组合的完成依赖（Completable Dependencies）：
    一个 CompletableFuture 的完成可以触发、依赖或组合其他 CompletableFuture 的完成.
    通过丰富的方法（如 thenApply(), thenCompose(), thenCombine(), thenAccept(), thenRun(), handle(), exceptionally(), whenComplete(), allOf(), anyOf() 等）,你可以声明式地定义：
        当这个 Future 完成时,接下来做什么？（使用其结果、处理其异常、触发另一个计算、组合另一个 Future 的结果等）.
        当多个 Future 都完成（或任意一个完成）时,接下来做什么？
    意义： 
        这种组合能力允许你构建复杂的、非阻塞的异步执行流水线（pipeline） 或执行图（graph）,清晰地表达任务之间的依赖关系、转换逻辑和错误处理路径,无需陷入繁琐的回调地狱（Callback Hell）或手动同步/等待.
3. 完成状态的传播（Completion Propagation）：
    在一个组合链中,一个 CompletableFuture 的完成（无论是正常还是异常）会自动触发它所依赖的下一个阶段的执行.这种完成状态的传播是构建流水线的基础.
    意义： 
        开发者只需关注每个阶段的任务逻辑和它们之间的连接关系,底层的线程调度和完成状态的传递由 CompletableFuture 框架高效处理. 
        
总结 “Completable” 的含义：
主动控制权： 赋予了开发者主动设置结果/异常（complete, completeExceptionally）的能力,结果来源不再局限于后台计算线程.
声明式组合： 提供了强大的 API 来声明式地组合多个异步操作,定义当一个或一组操作完成时接下来要执行的动作,形成流畅的异步流水线.
完成触发链： 一个 Future 的完成状态能够自动触发后续依赖操作的执行.


与传统 Future/FutureTask 的关键区别：
传统 Future/FutureTask:
    结果只能由执行任务的线程设置（通过 Callable.call() 或 Runnable.run() 的返回或抛出异常）.
    获取结果只能通过阻塞的 get() 或轮询 isDone().
    没有内置的机制来组合多个 Future 或定义完成后的回调.构建复杂依赖需要手动管理（例如,在一个 get() 之后启动另一个任务,或使用轮询）,代码容易变得复杂且易错.
CompletableFuture:
    主动完成： 任何线程都可以设置结果 (complete) 或异常 (completeExceptionally).
    非阻塞回调： 提供丰富的 thenXxx() 方法注册回调,在 Future 完成时自动、非阻塞地执行后续操作.
    组合与链式： 可以轻松地将多个异步操作串联、并联、转换结果、处理异常,形成清晰、声明式的异步工作流.
```

### 方法

```text
创建一个CompletableFuture,任务由ForkJoinPool.commonPool()执行

public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {}
Returns a new CompletableFuture that is asynchronously completed by a task running in the ForkJoinPool.commonPool() with the value obtained by calling the given Supplier.



对结果进行转换
public <U> CompletionStage<U> thenApply(Function<? super T,? extends U> fn);
Returns a new CompletionStage that, when this stage completes normally, is executed with this stage's result as the argument to the supplied function.



连接两个异步任务
public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);
Returns a new CompletionStage that is completed with the same value as the CompletionStage returned by the given function.
When this stage completes normally, the given function is invoked with this stage's result as the argument, returning another CompletionStage. 
When that stage completes normally, the CompletionStage returned by this method is completed with the same value.


Function以本CompletionStage的结果为入参且返参为另一个CompletionStage


合并两个任务的结果
public <U,V> CompletionStage<V> thenCombine (CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn);
Returns a new CompletionStage that, when this and the other given stage both complete normally, is executed with the two results as arguments to the supplied function. 
See the CompletionStage documentation for rules covering exceptional completion.
```

Why use thenCompose instead of thenApply?
```text
<U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)
<U>: the result type of the new stage.
fn: a function that takes the result of the current stage (T) and returns another CompletionStage<U>.

What it Does:
1. Waits for this stage to complete normally.
2. Applies fn to the result — this gives a new CompletionStage.
3. Waits for that CompletionStage to complete.
4. Completes the final stage with that result.
If the original stage completes exceptionally, fn is not called and the returned stage also completes exceptionally.

Why use thenCompose instead of thenApply?
If your function returns a CompletionStage, use thenCompose to flatten the nested stages into a single one.
thenApply → T → U (synchronous transformation)
thenCompose → T → CompletionStage<U> (asynchronous chaining)




```