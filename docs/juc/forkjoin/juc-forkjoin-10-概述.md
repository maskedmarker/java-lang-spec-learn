# juc-forkjoin

## 1. Fork/Join 是什么？解决什么问题？🔥

Fork/Join 框架(JDK7 引入)是 Java 并行计算框架，用于 把大任务拆分成多个小任务(fork)，并最终合并结果(join)，通常使用 分治算法(divide & conquer)。

核心思想：
Task -> Split into Subtasks -> execute in parallel -> combine result

```text
它擅长的场景：

场景	                           推荐
大量 CPU 计算、可拆分	          👍 非常适合
I/O 密集	                      ❌ 不适合
任务不可拆分	                  ❌ 没意义
```


##  2. 基础组件理解🔧
```text
| 类/接口             | 作用                     |
| ----------------   | ----------------------- |
| ForkJoinPool       | 线程池                   
| ForkJoinTask       | 可被提交的任务抽象             
| RecursiveAction    | 无返回值任务                 
| RecursiveTask<V>   | 有返回值任务     
            
Work-Stealing   线程“窃取”其他线程队列中的任务以保持高利用率 (工作线程的负载策略, 当自己的工作队列处理完了,从其他线程的工作队列获取待执行的工作任务)
```

```text
// ForkJoinTask实现了接口Future,代表一个异步计算
public abstract class ForkJoinTask<V> implements Future<V>, Serializable {}

// 继承自ForkJoinTask,代表一个异步计算,且提前说明无返回值
public abstract class RecursiveAction extends ForkJoinTask<Void> {}

// 继承自ForkJoinTask,代表一个异步计算,且提前说明有返回值
public abstract class RecursiveTask<V> extends ForkJoinTask<V> {}
```

## 3. 最小例子🌱

```text
public class SumTask extends RecursiveTask<Long> {
    private final long[] arr;
    private final int start, end;
    private static final int THRESHOLD = 10000;

    public SumTask(long[] arr, int start, int end) {
        this.arr = arr;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) sum += arr[i];
            return sum;
        }
        int mid = (start + end) >>> 1;
        SumTask left = new SumTask(arr, start, mid);
        SumTask right = new SumTask(arr, mid, end);
        left.fork();
        long rightResult = right.compute();
        long leftResult = left.join();
        return leftResult + rightResult;
    }

    public static void main(String[] args) {
        long[] arr = new long[10_000_000];
        Arrays.fill(arr, 1);
        ForkJoinPool pool = new ForkJoinPool();
        System.out.println(pool.invoke(new SumTask(arr, 0, arr.length)));
    }
}
```

## 4. 学习路线 🧠


```text
|   阶段   | 学习目标                                        |
| ------- | -----------------------------------------------|
| 入门     | 理解 split/join、RecursiveTask、RecursiveAction 
| 进阶     | 彻底理解 Work-Stealing、任务提交策略                   
| 源码     | 掌握 ForkJoinPool 的 runWorker & scan 操作       
| 性能     | 阈值选择、异步 vs 同步、work stealing 开销              
| 替代技术  | 与 Parallel Stream / CompletableFuture 对比    
```

## 5. 学习重点 📌

```text
✔ 阈值(threshold)决定性能上限

拆太细 → 开销巨大
拆太粗 → 并行不足
经验规则：一次 compute 执行 1–2ms 最优(依任务不同)



✔ fork() 顺序影响性能

left.fork();
right.compute();  // 推荐
left.join();
-------
left.fork();
right.fork();   // 不推荐


✔ pool 大小不是越大越好

CPU 计算型任务：
new ForkJoinPool(Runtime.getRuntime().availableProcessors())
```
## 6. 记住关键原则🔥

```text
任务应该足够大以保证并行化有意义
避免在任务间共享可变状态
合理设置阈值，平衡并行开销和收益
使用合适的异常处理机制
```

## 7. 和其他并发技术对比🔥

```text
| 技术                 | 优势                        | 适用        |
| ------------------  | -------------------------- | ----------- |
| ThreadPoolExecutor  | 通用                        | 普通并发任务   
| ForkJoin            | Work-Stealing，适合分治      | CPU-heavy 
| Parallel Stream     | 最简单的 fork/join 封装      | 快速并行处理集合  
| CompletableFuture   | 更灵活、异步编排              | I/O、复杂任务流 

```

## Dynamic Circular Work-Stealing Deque

基于双端队列数据结构(deque),无锁的(lock-free), 并支持work-stealing的负载均衡(load balancing)策略的调度算法,被广泛用于调度器.
fork-join框架本质也是一个调度器,所以底层使用调度器广泛使用的调度算法.

```text
Key Concepts
    Deque (Double-Ended Queue)
        Circular Array
        Dynamic Sizing
    Lock-Free
    
How It Works
    Local Operations: 
            A thread (owner) pushes and pops tasks from the bottom end of its own deque, treating it as a local stack for good cache locality.
    Stealing Operations: 
            An idle thread (thief) attempts to steal a task from the top end of another thread's deque.
    Dynamic Resizing: 
            If a push operation finds the current circular array is full, a new, larger array is allocated, and the elements are copied over. 
            The top and bottom pointers are adjusted to reference the new array, allowing the operation to complete without a hard limit on the deque size (other than system memory). 
            This process is synchronized to ensure concurrent steal attempts either abort or access a valid state.     
```

```text
证明动态环形工作窃取双端队列的正确性是一项复杂的任务.这个算法(通常称为 Chase-Lev 算法的一个变种)已被计算机科学界的顶级专家和形式化验证工具证明是正确的，是现代并行运行时系统的基石。
证明的主要挑战
    如果您想了解证明的核心逻辑框架，我可以概述其关键步骤，但请注意，实际证明需要严格的数学符号和原子操作的详细分析：
        定义抽象状态: 将动态数组抽象为一个无限增长的序列。
        定义操作的线性化点: 确定每个并发操作(Push、Pop、Steal、Resize)在哪里“看起来”是在顺序执行的。例如，Steal 操作的线性化点通常是成功执行 CAS 修改 top 指针的那一刻。
        证明不变性: 证明在所有并发操作执行前后，top 和 bottom 指针之间的任务集合始终满足一致性(例如，没有重叠或丢失)。
        证明任务唯一性: 证明一旦一个线程成功“声称”一个任务(通过原子地修改 top 或 bottom)，其他线程不可能同时声称同一个任务。
        证明活性: 证明只要队列非空，且有线程尝试窃取，任务最终会被移除。


证明方法与技术        
唯一性: 一个任务被推入后，不能被取出超过一次(不能被一个拥有者弹出，同时又被另一个窃贼窃取)
不变性条件 (Invariants): 证明围绕保持数据结构在各种操作前后的一致性展开。例如，一个关键的不变性是 bottom 索引始终大于 top 索引(除非队列为空)，并且这两个索引定义了当前数组中的有效范围。  
线性化(Linearizability): 动态双端队列通常通过证明其是可线性化的来实现正确性保证。线性化是一种很强的正确性规范，它要求并发操作的效果等同于这些操作以某种顺序(该顺序符合它们的实时时间顺序)在抽象的顺序数据结构上执行。

简化版的CircularWSDeque代码中给出了操作的线性化点,比较清晰给出了大概的线性化顺序.
```