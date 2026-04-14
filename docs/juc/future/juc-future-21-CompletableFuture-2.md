# juc-future-CompletableFuture




## CompletableFuture源码分析

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    
    // 当result==null时,当前计算阶段还未完成(所以null值需要被wrap到AltResult里; 同时为了存储当前计算阶段的计算任务执行时发生的异常,该异常也需要被wrap到AltResult里)
    volatile Object result;
    
    // 使用CAS-based lock-free Treiber stack存储当前计算阶段完成时的回调(completion-action)
    volatile Completion stack;
}

/**
 completion-action可以是纯粹的回调.同时该completion-action还可以同时负责下游计算阶段的计算任务的执行.🚀🚀🚀
 Completion作为回调的completion-action,其必须继承自Runnable.
 Completion作为回调,只要求在当前计算阶段完成后执行(强调先后顺序),并没有要求在哪个线程中执行.所以允许同步和异步执行.
 Completion同步执行就意味着,当前计算阶段result被设置后紧接着执行completion.
 Completion异步执行就意味着,当前计算阶段result被设置后紧接着completion被提交到线程池中.
*/
```

```java
abstract static class Completion extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
    volatile Completion next;      // Treiber stack link

    // Performs completion action if triggered, returning a dependent that may need propagation, if one exists. 💯💯💯
    abstract CompletableFuture<?> tryFire(int mode);
    
    abstract boolean isLive();   // Returns true if possibly still triggerable
    public final void run()                { tryFire(ASYNC); }                         // 被普通线程池执行时
    public final boolean exec()            { tryFire(ASYNC); return true; }            // 被ForkJoinPool线程池执行时
    public final Void getRawResult()       { return null; }
    public final void setRawResult(Void v) {}                                          // Completion自身不需要储存结果
}

/**
 
 AsynchronousCompletionTask是marker interface,用于监控.
 Completion继承自ForkJoinTask,是为了让completion不仅能被用户自定义线程池执行,还可以被ForkJoinPool执行. 
 ForkJoinTask<Void>的Void是因为Doug Lea的个人设计风格(Completion自身不需要储存结果). 
       当作为计算阶段的计算任务的Completion被执行时,采用 [当前计算阶段.xxx(上游计算阶段)] 的代码风格
            比如 thenAccept, 
                UniAccept作为上游的completion-action,在上游完成后会触发tryFire
                在UniAccept的tryFire方法中, 调用d.uniAccept(a, fn)   其中,d是a的下游,fn是d的计算任务
                d.uniAccept(a, fn)执行中,fn的结果直接设置到d.result,
*/

/**
 * tryFire支持多个模式,针对的是当前completion-action还负责下游计算任务的执行时,该如何调度下游计算任务 (Completion衔接上下游,如何调度下游的执行)
 * SYNC(同步模式)    当前计算阶段的result设置后,紧接着执行下游计算任务
 * ASYNC(异步模式)   当前计算阶段的result设置后,紧接着让线程池异步执行下游计算任务
 * NESTED(嵌套模式)  类似同步模式
 */
```


### tryFire

```text
Completion.tryFire()的返回值是一个“调度指令”.

它不是：
是否执行成功
是否真的运行了用户代码
而是一个调度控制信号.


🚀返回 null：
表示：当前Completion还不能完成,或未能成功触发,需要保留,之后再试.

常见于：
上游future还没完成
claim()失败(被别的线程抢走)
异步执行已经提交,当前线程无需继续



🚀返回 this
表示：当前Completion已经执行完成(也即且其归属的CompletableFuture计算阶段也完成了),且其归属的CompletableFuture的其他Completion也不需要继续触发.

常见于：
当前阶段只是一个中间触发点
不需要连锁传播



🚀返回 非 null 且非 this 的 CompletableFuture(最常见)
表示：当前Completion已完成(也即且其归属的CompletableFuture计算阶段也完成了),并且这个返回的CompletableFuture可能还有后续Completion需要触发.
```

### postComplete

postComplete方法在CompletableFuture.result设置完后才被调用,用来执行completion-actions.

CompletableFuture.postComplete方法的并发安全来自于CompletableFuture.stack是Treiber stack.

```text
final void postComplete() {
    CompletableFuture<?> f = this;  // 起始点为当前完成的 future
    Completion h;  // 当前处理的 Completion 节点
    
    // 循环条件：检查当前 future (f) 或其回退路径是否有待处理节点
    while ((h = f.stack) != null ||     // 条件1：当前 f 有依赖栈
           (f != this && (h = (f = this).stack) != null)) {  // 条件2：回退到起始点检查
        CompletableFuture<?> d;  // tryFire 返回的新 future
        Completion t;           // 临时保存 h.next
        
        
        if (f.casStack(h, t = h.next)) {                                        // CAS弹出栈顶节点h(并发的线性化点)
            // 情况1：栈中仍有其他节点 (t != null)
            if (t != null) {
                if (f != this) { 
                    pushStack(h);                                                  // 将 h 压回起始点栈
                    continue;                                                      // 重新处理当前 f 的剩余节点
                }
                h.next = null;                                                     // 断开链表 (防止错误引用)
            }
            
            
            f = (d = h.tryFire(NESTED)) == null ?                            // 触发h,h产生的新future(d也依赖当前Future的完成,此时也需要触发d)
                 this :    // 无新 future 则回退到起始点
                 d;        // 有新 future 则转向处理它
        }
    }
}

初始化变量：f 初始化为当前CompletableFuture(即刚刚完成的那一个),h 用来指向当前要处理的Completion节点.
    CompletableFuture<?> f = this
    h = f.stack

循环条件：while ((h = f.stack) != null || (f != this && (h = (f = this).stack) != null))
这个循环条件的意思是：只要当前f的stack不为空,或者当前f不是最初的那个(即已经切换到其他依赖的Future)并且最初的那个Future的stack不为空,就继续循环.
 注意：这里有两个条件,第一个条件检查当前f的stack,第二个条件是在第一个条件不满足(即当前f的stack为空)且f不是this(即已经处理到了某个后续操作产生的Future)时,重新回到最初的this(即最初完成的那个Future)检查其stack是否还有未处理的Completion.这样设计是为了确保所有依赖链上的后续操作都被处理.





设计意图💯💯💯
深度优先处理：
    优先处理依赖链最下游的 future,再回溯上游.
避免递归爆栈：
    用循环代替递归,处理任意深度的依赖链.
无锁并发：
    通过 CAS 实现线程安全的栈操作.
状态回溯：
    通过 f != this 判断和栈转移,确保所有节点最终被处理.
    
    
    
关键常量 NESTED
作用：标记当前触发是嵌套在完成传播中
与模式对比：
  SYNC(0)：同步立即执行
  ASYNC(1)：异步提交线程池
  NESTED(-1)：嵌套传播中触发,根据情况选择执行方式
在 tryFire(NESTED) 中通常会尝试直接执行(类似 SYNC),避免额外排队.    
```

```text
abstract CompletableFuture<?> tryFire(int mode);

cf-A
    completion-A1
        cf-B

cf-A.postComplete开始依次触发自己的completion
理解completion.tryFire的返回值是关键:
当返回null时, 表示当前completion-A1没有被其他CompletableFuture依赖,即不需要讲completion事件告诉其他CompletableFuture依赖者.
当返回非null时, 表示当前这个completion-A1有被其他cf-B依赖,即需要讲completion事件告诉cf-B依赖者.
            当前这个completion-A1先通过completeValue设置cf-B依赖者
            当前这个completion-A1先通过cf-B依赖者的postFire检查cf-B的completion-stack是否为空,如果不为空就将cf-B返回给cf-A的postComplete(即当前线程接着处理cf-B的completion-stack)

cf-x形成了一个依赖树,使用深度优先的算法来执行根到叶子的completion💯

注意:postFire的入参mode是NESTED,所以cf-B的postFire不会协助完成cf-A的completion-stack
```
