# juc-future-2


## CompletableFuture源码分析

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    // Either the result or boxed AltResult
    volatile Object result;
    // Top of Treiber stack of dependent actions 又是CAS-based stack,保存dependent actions
    volatile Completion stack;
}
```

### Completion
```text
`CompletableFuture`使用一种称为“依赖栈”（或“依赖链表”）的结构来管理这些依赖关系。当一个`CompletableFuture`完成时，它会触发所有依赖于它的后续操作（也就是那些通过`then*`方法注册的操作）。
这些后续操作被封装在一个个的“Completion”对象中，并且这些Completion对象会被压入当前CompletableFuture的栈（或者链表）中。

在`CompletableFuture`的源码中，有几个重要的Completion子类：
- `UniCompletion`：表示一元依赖关系的基类，即依赖于单个CompletableFuture的完成。
- `BiCompletion`：表示二元依赖关系，依赖于两个CompletableFuture的完成（例如`thenCombine`）。
- `CoCompletion`：用于组合多个CompletableFuture（例如`allOf`）。
```

UniCompletion
```text
具体到`UniCompletion`，它是那些只依赖一个源`CompletableFuture`的Completion的基类。
例如，`thenApply`、`thenAccept`、`thenRun`等方法都会创建一个`UniCompletion`的子类实例。
`UniCompletion`继承自`Completion`，它有两个重要的属性：
1. `src`：它所依赖的源`CompletableFuture`。当这个源完成时，这个Completion就会被触发。
2. `executor`：一个可选的`Executor`，用于指定执行后续操作的线程池。如果没有指定，则可能使用调用者线程或ForkJoinPool。

`UniCompletion`的作用是：
- 它封装了一个依赖于单个源CompletableFuture完成的操作（函数）。
- 当源CompletableFuture完成时，它会尝试出栈（即从源的依赖栈中移除）并执行后续操作（即调用其`tryFire`方法）。
- 后续操作执行完成后，它会把结果传递给新创建的CompletableFuture（即通过`then*`方法返回的那个）。

举个例子，当我们写：
CompletableFuture<String> base = new CompletableFuture<>();
CompletableFuture<Integer> next = base.thenApply(s -> s.length());
在调用`thenApply`时，会创建一个`UniApply`对象（它是`UniCompletion`的子类），这个对象中包含了函数`s -> s.length()`，并且这个对象会被压入`base`的依赖栈中。
当`base`完成时（比如通过`complete("hello")`），它会遍历自己的依赖栈，执行每个Completion（这里就是那个`UniApply`对象）的`tryFire`方法，从而触发函数计算，并将结果设置到`next`这个CompletableFuture中。


核心作用

1. 封装依赖关系
    UniCompletion 记录了一个源 CompletableFuture（src属性）。当源任务完成时，它会自动触发后续操作（如函数计算、结果传递）。
2. 管理后续操作
    子类（如 UniApply、UniAccept）通过实现 tryFire() 方法，定义了源任务完成后具体要执行的逻辑（如应用函数、消费结果）。
3. 实现非阻塞回调
    当源任务完成时，UniCompletion 会通过内部栈（stack）被弹出并执行，确保后续操作在正确的线程（原线程或指定线程池）中触发。
4. 处理结果传递
    负责将源任务的结果传递给后续操作，并设置新 CompletableFuture 的结果（或异常）。
```

```java
abstract static class UniCompletion<T,V> extends Completion {
    Executor executor;      // 执行后续操作的线程池（可能为null）
    CompletableFuture<V> dep;  // 依赖此操作的新CompletableFuture
    CompletableFuture<T> src;   // 源CompletableFuture

    // 核心方法：尝试触发后续操作
    abstract CompletableFuture<V> tryFire(int mode);

    // 判断当前操作是否可执行
    final boolean claim() {
        Executor e = executor;
        if (e != null) {
            executor = null; // 防止重复执行
            e.execute(this); // 提交到线程池
            return true;
        }
        return false;
    }
}
```

工作流程
```text
以 thenApplyAsync 为例：

1. 创建 UniCompletion
    调用 thenApplyAsync(fn, executor) 时，生成一个 UniApply 对象（UniCompletion 子类），包含：
        src：当前源 CompletableFuture
        dep：新的 CompletableFuture（用于返回给用户）
        fn：待应用的函数
        executor：指定的线程池
2. 压入依赖栈
    将 UniApply 对象压入源任务的内部栈（stack 字段）。
3. 触发执行
    当源任务完成时：
        遍历其栈中所有 UniCompletion。
        调用 tryFire() 方法
        在 tryFire() 中：
            通过 claim() 判断是否需要线程池执行。
            应用函数 fn 计算新结果。
            将结果设置给 dep（新 CompletableFuture）。        
```

子类示例
```text
子类	            对应方法	              行为
UniApply	    thenApply[Async]	  应用函数 Function<T,U>
UniAccept	    thenAccept[Async]	  消费结果 Consumer<T>
UniRun	        thenRun[Async]	      执行动作 Runnable
UniCompose	    thenCompose[Async]	  组合新异步任务
```

CompletableFuture的方法postComplete, 详细解释
```text
final void postComplete() {
    CompletableFuture<?> f = this;  // 起始点为当前完成的 future
    Completion h;  // 当前处理的 Completion 节点
    
    // 循环条件：检查当前 future (f) 或其回退路径是否有待处理节点
    while ((h = f.stack) != null ||     // 条件1：当前 f 有依赖栈
           (f != this && (h = (f = this).stack) != null)) {  // 条件2：回退到起始点检查
        CompletableFuture<?> d;  // tryFire 返回的新 future
        Completion t;           // 临时保存 h.next
        
        // CAS 弹出栈顶节点 h (避免并发修改)
        if (f.casStack(h, t = h.next)) {
            // 情况1：栈中仍有其他节点 (t != null)
            if (t != null) {
                if (f != this) { 
                    pushStack(h);  // 将 h 压回起始点栈
                    continue;     // 重新处理当前 f 的剩余节点
                }
                h.next = null;  // 断开链表 (防止错误引用)
            }
            
            // 触发 h 并获取其产生的新 future (d)
            f = (d = h.tryFire(NESTED)) == null ? 
                 this :    // 无新 future 则回退到起始点
                 d;        // 有新 future 则转向处理它
        }
    }
}

初始化变量：f 初始化为当前CompletableFuture（即刚刚完成的那一个），h 用来指向当前要处理的Completion节点。
    CompletableFuture<?> f = this
    h = f.stack

循环条件：while ((h = f.stack) != null || (f != this && (h = (f = this).stack) != null))
这个循环条件的意思是：只要当前f的stack不为空，或者当前f不是最初的那个（即已经切换到其他依赖的Future）并且最初的那个Future的stack不为空，就继续循环。
 注意：这里有两个条件，第一个条件检查当前f的stack，第二个条件是在第一个条件不满足（即当前f的stack为空）且f不是this（即已经处理到了某个后续操作产生的Future）时，重新回到最初的this（即最初完成的那个Future）检查其stack是否还有未处理的Completion。这样设计是为了确保所有依赖链上的后续操作都被处理。





设计意图
深度优先处理：
    优先处理依赖链最下游的 future，再回溯上游。
避免递归爆栈：
    用循环代替递归，处理任意深度的依赖链。
无锁并发：
    通过 CAS 实现线程安全的栈操作。
状态回溯：
    通过 f != this 判断和栈转移，确保所有节点最终被处理。
    
    
    
关键常量 NESTED
作用：标记当前触发是嵌套在完成传播中
与模式对比：
  SYNC（0）：同步立即执行
  ASYNC（1）：异步提交线程池
  NESTED（-1）：嵌套传播中触发，根据情况选择执行方式
在 tryFire(NESTED) 中通常会尝试直接执行（类似 SYNC），避免额外排队。    
```