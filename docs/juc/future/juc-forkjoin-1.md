# juc-forkjoin


## ForkJoinPool
ForkJoinPool 是 Java 7 引入的高性能并行任务执行框架，用于高效执行可分解的任务（通常是递归任务）,专为分治算法（Divide-and-Conquer）设计，核心采用工作窃取（Work-Stealing）算法。
它是java.util.concurrent包的核心组件，支撑了Java 8+的Stream并行流和 CompletableFuture。

⚠️ 注意:ForkJoinPool并不是为Stream API准备的.

```text
一、核心设计思想
分治策略（Fork-Join 模型）
    Fork：将大任务递归拆分为子任务（fork()）
    Join：合并子任务结果（join()）
    示例：归并排序、快速排序、矩阵运算

工作窃取（Work-Stealing）
    每个工作线程维护一个双端队列（Deque）
    线程优先处理自己队列头部的任务（LIFO）
    空闲线程从其他队列尾部窃取任务（FIFO）
    优势：减少线程竞争，最大化 CPU 利用率
 

  
二、核心组件
ForkJoinTask
    抽象基类，代表可分解任务
    子类：
        RecursiveAction：无返回值任务（如排序）
        RecursiveTask：有返回值任务（如求和）
ForkJoinWorkerThread
    工作线程基类，关联一个 WorkQueue
WorkQueue
    内部双端队列：
        头部：线程本地任务（LIFO）  (fork时,将新任务放到队列的头,以达到优先执行任务树中同层级的任务)
        尾部：供其他线程窃取（FIFO） (降低抢任务时发生锁竞争的概率)



三、关键机制
任务调度流程
    参见images/juc-forkjoin.png
并行度（Parallelism）
    默认值 = Runtime.getRuntime().availableProcessors() - 1
公共池（Common Pool）
    Java 8+ 全局共享的 ForkJoinPool
    访问方式：ForkJoinPool.commonPool()
    并行度配置：
        JVM 参数：-Djava.util.concurrent.ForkJoinPool.common.parallelism=N
        代码中设置：System.setProperty("java.util.concurrent...", "N")
特殊容错机制
    当并行度=0 时（如线程被禁用），强制视为 parallelism=1
    保证至少使用调用者线程（Caller-Runs） 执行任务
    
 
    
四、最佳实践
适用场景
    递归可分解任务（树/图遍历）
    CPU 密集型计算
    无阻塞 I/O 的操作（避免线程饥饿）

避坑指南
    避免阻塞：线程池大小=CPU核心数，阻塞会导致性能崩溃
    任务粒度：子任务执行时间 > 100μs（避免调度开销）
    结果合并：join() 应在任务拆分后调用（防止死锁）
    避免同步：使用 Phaser 替代 CountDownLatch
与 ThreadPoolExecutor 对比
特性	         ForkJoinPool	    ThreadPoolExecutor
任务队列	     工作窃取双端队列	    阻塞单队列
任务类型	     递归可分解任务	    独立任务
线程利用率	 高（自动负载均衡）	依赖任务分配
默认线程数	 CPU核心数-1	        无固定规则



总结
ForkJoinPool 通过分治策略+工作窃取实现了：
    高吞吐量：自动负载均衡
    低竞争：本地队列优先处理
    资源高效：线程数≈CPU核心数
    优雅降级：并行度=0时转为串行执行
⚠️ 注意：不适合 I/O 密集型任务（考虑使用虚拟线程或混合线程池）


```

```text
static final int commonParallelism;

This code snippet defines a static final integer commonParallelism related to the common pool's parallelism in Java's ForkJoinPool framework. 

Purpose:
The commonParallelism field represents the number of threads available in the common ForkJoinPool.
It's used to configure the default level of parallelism for parallel operations in Java's Stream API and other ForkJoinPool-based operations.

Special Handling:
When common pool threads are disabled (common.parallelism = 0), the code still reports parallelism as 1.
This ensures that even when parallelism is technically zero, operations will still work using "caller-runs" mechanics (where the calling thread executes the task).

Behavior:
Normally, this would equal the number of available processors minus 1 (Runtime.getRuntime().availableProcessors() - 1).
The minimum value is always 1, even if parallelism is disabled, to maintain basic functionality.

Usage:
This value is used internally by the ForkJoinPool to determine how many threads to use for parallel operations.
It affects parallel streams and other parallel operations that use the common pool.
```

### helpQuiescePool

```text
This method is internal to the ForkJoinPool framework (not part of the public API). 
It is used by worker threads to help the pool reach a quiescent state (no active tasks) before blocking or terminating. 
The WorkQueue parameter is typically the worker's own queue.

Typical Behavior:
    Scan for Tasks:      The worker thread attempts to steal and execute tasks from other queues to help drain the pool.
    Wait for Quiescence: If no tasks are found, it may park (block) for up to nanos nanoseconds.
    Handle Interruption: If interruptible=true, it checks for Thread.interrupted() and may abort early.
    
    
Key Notes:
    Not for Public Use: This is an implementation detail of ForkJoinPool (e.g., in ForkJoinPool.java in OpenJDK).
    Quiescence:  Ensures all tasks are complete before workers are retired or the pool shuts down.
    Performance: Critical for efficient task execution in work-stealing frameworks.
If you're debugging or modifying ForkJoinPool internals, this method is where workers "help" each other finish remaining tasks.    
```