# juc-forkjoin


## ForkJoinPool
ForkJoinPool是 Java7引入的高性能并行任务执行框架.
Java8的Stream并行流和CompletableFuture的默认线程池都是ForkJoinPool.

```text
javadoc描述了ForkJoinPool的主要特征:
An ExecutorService for running ForkJoinTasks. (ForkJoinPool是执行ForkJoinTask的ExecutorService)
A ForkJoinPool provides the entry point for submissions from non-ForkJoinTask clients, as well as management and monitoring operations.(提供了提交ForkJoinTask的API接口)
A ForkJoinPool differs from other kinds of ExecutorService mainly by virtue of employing work-stealing (普通的ExecutorService只有一个工作队列, ForkJoinPool中每个线程都有自己的工作队列,且支持work-stealing的负载策略)
    all threads in the pool attempt to find and execute tasks submitted to the pool and/or created by other active tasks (eventually blocking waiting for work if none exist).
```

```text
工作窃取(Work-Stealing)
    每个工作线程维护一个双端队列(Deque),线程优先处理自己队列尾部的任务(LIFO),空闲线程从其他队列头部窃取任务(FIFO)
    通过在两端操作队列元素的方式降低并发的同步开销.



三、关键机制
任务调度流程
    参见images/juc-forkjoin.png


    

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