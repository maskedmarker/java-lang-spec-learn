# juc-future-CompletableFuture

javadoc和后面example分析交叉看,CompletableFuture理解起来比较抽象.


## CompletionStage javadoc

```text
A stage of a possibly asynchronous computation, that performs an action or computes a value when another CompletionStage completes. 
A stage completes upon termination of its computation, but this may in turn trigger other dependent stages. 

The functionality defined in this interface takes only a few basic forms, which expand out to a larger set of methods to capture a range of usage styles:
The computation performed by a stage may be expressed as a Function, Consumer, or Runnable (using methods with names including apply, accept, or run, respectively) depending on whether it requires arguments and/or produces results. 
For example, stage.thenApply(x -> square(x)).thenAccept(x -> System.out.print(x)).thenRun(() -> System.out.println()). 
An additional form (compose) applies functions of stages themselves, rather than their results.(💯)

One stage's execution may be triggered by completion of a single stage, or both of two stages, or either of two stages. 

Dependencies on a single stage are arranged using methods with prefix then. 
Those triggered by completion of both of two stages may combine their results or effects, using correspondingly named methods. 
Those triggered by either of two stages make no guarantees about which of the results or effects are used for the dependent stage's computation.
Dependencies among stages control the triggering of computations, but do not otherwise guarantee any particular ordering. 
Additionally, execution of a new stage's computations may be arranged in any of three ways: 
default execution, 
default asynchronous execution (using methods with suffix async that employ the stage's default asynchronous execution facility), 
or custom (via a supplied Executor). 
The execution properties of default and async modes are specified by CompletionStage implementations, not this interface. 
Methods with explicit Executor arguments may have arbitrary execution properties, and might not even support concurrent execution, but are arranged for processing in a way that accommodates asynchrony.

Two method forms support processing whether the triggering stage completed normally or exceptionally: 
Method whenComplete allows injection of an action regardless of outcome, otherwise preserving the outcome in its completion. 
Method handle additionally allows the stage to compute a replacement result that may enable further processing by other dependent stages. 
In all other cases, if a stage's computation terminates abruptly with an (unchecked) exception or error, then all dependent stages requiring its completion complete exceptionally as well, with a CompletionException holding the exception as its cause. 
If a stage is dependent on both of two stages, and both complete exceptionally, then the CompletionException may correspond to either one of these exceptions. 
If a stage is dependent on either of two others, and only one of them completes exceptionally, no guarantees are made about whether the dependent stage completes normally or exceptionally. 
In the case of method whenComplete, when the supplied action itself encounters an exception, then the stage exceptionally completes with this exception if not already completed exceptionally.
All methods adhere to the above triggering, execution, and exceptional completion specifications (which are not repeated in individual method specifications). 
Additionally, while arguments used to pass a completion result (that is, for parameters of type T) for methods accepting them may be null, passing a null value for any other parameter will result in a NullPointerException being thrown.


This interface does not define methods for initially creating, forcibly completing normally or exceptionally, probing completion status or results, or awaiting completion of a stage. 
Implementations of CompletionStage may provide means of achieving such effects, as appropriate. Method toCompletableFuture enables interoperability among different implementations of this interface by providing a common conversion type.
```



## CompletableFuture javadoc

```text
A Future that may be explicitly completed (setting its value and status), and may be used as a CompletionStage, supporting dependent functions and actions that trigger upon its completion.
(CompletableFuture的突出功能有2个:一是可以手动设置result达到完成complete, 二是支持完成时的回调.)
```

```text
When two or more threads attempt to complete, completeExceptionally, or cancel a CompletableFuture, only one of them succeeds.(手动complete是并发安全的,且只能成功一次.)

In addition to these and related methods for directly manipulating status and results, CompletableFuture implements interface CompletionStage with the following policies:
Actions supplied for dependent completions of non-async methods may be performed by the thread that completes the current CompletableFuture, or by any other caller of a completion method.
(使用CompletableFuture的同步方法设置的completion actions由调用complete方法的调用方的线程完成completion actions)
All async methods without an explicit Executor argument are performed using the ForkJoinPool.commonPool() (unless it does not support a parallelism level of at least two, in which case, a new Thread is created to run each task). 
(使用CompletableFuture的异步方法设置的completion actions由线程池完成)
(可以理解为同步completion action和异步completion action)

To simplify monitoring, debugging, and tracking, all generated asynchronous tasks are instances of the marker interface CompletableFuture.AsynchronousCompletionTask.(AsynchronousCompletionTask是marker interface)
All CompletionStage methods are implemented independently of other public methods, so the behavior of one method is not impacted by overrides of others in subclasses.
```

```text
CompletableFuture also implements Future with the following policies:
Since (unlike FutureTask) this class has no direct control over the computation that causes it to be completed, cancellation is treated as just another form of exceptional completion. 
Method cancel has the same effect as completeExceptionally(new CancellationException()). 
Method isCompletedExceptionally can be used to determine if a CompletableFuture completed in any exceptional fashion.
In case of exceptional completion with a CompletionException, methods get() and get(long, TimeUnit) throw an ExecutionException with the same cause as held in the corresponding CompletionException. 
To simplify usage in most contexts, this class also defines methods join() and getNow that instead throw the CompletionException directly in these cases.

```

```text
Overview:

A CompletableFuture may have dependent completion actions(💯),collected in a linked stack. (CompletableFuture.Completion感觉命令为CompletableFuture.CompletionAction更加直观) (dependent这里指的是completion actions的执行依赖于CompletableFuture的完成,dependent可以翻译为后续的)

It atomically completes by CASing a result field, and then pops off and runs those actions. This applies across normal vs exceptional outcomes, sync vs async actions, binary triggers, and various forms of completions.
Non-nullness of field result (set via CAS) indicates done.  An AltResult is used to box null as a result, as well as to hold exceptions.  
Using a single field makes completion simple to detect and trigger.  
Encoding and decoding is straightforward but adds to the sprawl of trapping and associating exceptions with targets. 
Minor simplifications rely on (static) NIL (to box null results) being the only AltResult with a null exception field, so we don't usually need explicit comparisons.
Even though some of the generics casts are unchecked (see SuppressWarnings annotations), they are placed to be appropriate even if checked.

Dependent actions(💯) are represented by Completion objects linked as Treiber stacks headed by field "stack". 
There are Completion classes for each kind of action, grouped into 
single-input (UniCompletion), two-input (BiCompletion), projected (BiCompletions using either (not both) of two inputs), shared (CoCompletion, used by the second of two sources), zero-input source actions, and Signallers that unblock waiters. 
Class Completion extends ForkJoinTask to enable async execution(adding no space overhead because we exploit its "tag" methods to maintain claims). It is also declared as Runnable to allow usage with arbitrary executors.


Support for each kind of CompletionStage relies on a separate class, along with two CompletableFuture methods:
1. A Completion class with name X corresponding to function, prefaced with "Uni", "Bi", or "Or". Each class contains fields for source(s), actions, and dependent. 
   They are boringly similar, differing from others only with respect to underlying functional forms. We do this so that users don't encounter layers of adaptors in common usages. 
   We also include "Relay"(💯) classes/methods that don't correspond to user methods; they copy results from one stage to another.

2. Boolean CompletableFuture method x(...) (for example uniApply) takes all of the arguments needed to check that an action is triggerable, 
   and then either runs the action or arranges its async execution by executing its Completion argument, if present. The method returns true if known to be complete.

3. Completion method tryFire(int mode) invokes the associated x method with its held arguments, and on success cleans up. 
   The mode argument allows tryFire to be called twice (SYNC, then ASYNC); the first to screen and trap exceptions while arranging to execute, and the second when called from a task. 
   (A few classes are not used async so take slightly different forms.)  The claim() callback suppresses function invocation if already claimed by another thread.

4. CompletableFuture method xStage(...) is called from a public stage method of CompletableFuture x. It screens user arguments and invokes and/or creates the stage object.  
   If not async and x is already complete, the action is run immediately.  Otherwise a Completion c is created, pushed to x's stack (unless done), and started or triggered via c.tryFire.  
   This also covers races possible if x completes while pushing.  
   Classes with two inputs (for example BiApply) deal with races across both while pushing actions.  
   The second completion is a CoCompletion pointing to the first, shared so that at most one performs the action.  The multiple-arity methods allOf and anyOf do this pairwise to form trees of completions.

Note that the generic type parameters of methods vary according to whether "this" is a source, dependent, or completion.

Method postComplete is called upon completion unless the target is guaranteed not to be observable (i.e., not yet returned or linked). 
Multiple threads can call postComplete, which atomically pops each dependent action, and tries to trigger it via method tryFire, in NESTED mode.  
Triggering can propagate recursively, so NESTED mode returns its completed dependent (if one exists) for further processing by its caller (see method postFire).

Blocking methods get() and join() rely on Signaller Completions that wake up waiting threads.  
The mechanics are similar to Treiber stack wait-nodes used in FutureTask, Phaser, and SynchronousQueue. See their internal documentation for algorithmic details.

Without precautions, CompletableFutures would be prone to garbage accumulation as chains of Completions build up, each pointing back to its sources. So we null out fields as soon as possible (see especially method Completion.detach). 
(如果不采取预防措施,CompletableFutures 就容易出现垃圾堆积的情况,因为随着完成链的不断累积,每个环节都会指向其源头)

The screening checks needed anyway harmlessly ignore null arguments that may have been obtained during races with threads nulling out fields. 
We also try to unlink fired Completions from stacks that might never be popped (see method postFire). 
Completion fields need not be declared as final or volatile because they are only visible to other threads upon safe publication.
```


## 设置理念
```text
CompletableFuture的设计目标是所有阶段都必须是 可排队、可转移、可重试、可窃取 的“节点”.

为什么不能直接在thenAccept()里执行Consumer?
问题非常多：
    执行线程不可控
    可能执行多次
    竞态下无法保证happens-before
    无法和ForkJoinPool/async机制统一
    无法被work-stealing调度

UniAccept的存在价值,不在于“保存一个 Consumer”,而在于：它是CompletableFuture中“单输入、无返回值阶段”的并发调度与幂等执行载体.

UniAccept 具体解决了哪些“硬问题”？
① 并发下“只执行一次”  (UniAccept.claim())
② 延迟绑定执行线程
③ 阶段是“可转移的”
④ 异常传播的统一入口

UniAccept 的价值在于“它作为一个对象存在,使得 CompletableFuture 的阶段可以被并发调度、抢占执行、且严格只执行一次”.
```


## CompletableFuture

FutureTask 是在 Java 5 (JDK 1.5) 中引入的,它是随着 java.util.concurrent 包一起发布的.
CompletableFuture 是 Java 8 引入的一个强大的异步编程工具,它实现了 Future 接口,并提供了丰富的 API 来支持异步任务的组合、链式调用、异常处理等功能.
相比传统的 Future,CompletableFuture 更加灵活,适合构建复杂的异步流程.


重要提醒💯💯💯
1. CompletableFuture默认使用ForkJoinPool.commonPool()线程池(处理的线程个数是电脑CPU核数-1).一般建议使用自定义线程池,优化线程池配置参数,同时做好不同业务的线程池隔离.
2. 需要特别注意自定义线程池的拒绝策略.
   DiscardPolicy或者DiscardOldestPolicy,当线程池饱和时,会直接丢弃任务,不会抛弃异常.因此建议,CompletableFuture线程池策略最好使用AbortPolicy,然后耗时的异步线程,做好线程池隔离.
3. 要注意父子任务通过join的等待,需要将父子任务放入不同线程池,防止可以线程被耗尽导致无穷等待.
4. RPC中返回结果是由IO线程负责设置的,不要使用CompletableFuture同步方法,这会占用宝贵的IO线程资源,应该使用CompletableFuture异步方法.




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