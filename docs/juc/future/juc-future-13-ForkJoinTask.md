# juc-ForkJoinTask

## ForkJoinTask

```text
public abstract class ForkJoinTask<V> implements Future<V>, Serializable {
    public abstract V getRawResult();
    protected abstract void setRawResult(V value);
    rotected abstract boolean exec();
}


```



```text

volatile int status; // accessed directly by pool and workers

static final int DONE_MASK   = 0xf0000000;  // mask out non-completion bits
static final int NORMAL      = 0xf0000000;  // must be negative
static final int CANCELLED   = 0xc0000000;  // must be < NORMAL
static final int EXCEPTIONAL = 0x80000000;  // must be < CANCELLED
static final int SIGNAL      = 0x00010000;  // must be >= 1 << 16
static final int SMASK       = 0x0000ffff;  // short bits for tags

32-bits中,高16-bits用来做control-bits,低16-bits是留给用户自定义使用的.
高16-bits中的最低1-bits是signal位;
高16-bits中的最高1-bits是1时表示task已经是终态,最高1-bits是0时表示初始态.


status 字段用于保存运行控制状态位，这些状态位被打包在一个 int 中，以最小化内存占用，并通过 CAS 操作保证原子性。
status 的初始值为 0，在任务完成之前保持非负值。
当任务完成后，(status & DONE_MASK) 的结果会是以下三种之一：
NORMAL（正常完成）、CANCELLED（被取消）或 EXCEPTIONAL（异常终止）。
这些控制位仅占用 status 字段的高半部分（16 位中的部分）。而低位部分则留作用户自定义标签（user-defined tags）使用。

当某个任务被其他线程以阻塞方式等待时，其 status 会被设置上 SIGNAL 位。如果一个被“窃取”的任务在完成时发现自己带有 SIGNAL 标志，它会通过调用 notifyAll 唤醒所有等待的线程。




```


```text
一个“主” ForkJoinTask 可以通过以下方式开始执行：
    被显式地提交（submit）到某个 ForkJoinPool；
    或者在当前线程尚未参与任何 ForkJoin 计算时，通过调用 ForkJoinPool.commonPool() 的 fork()、invoke() 或相关方法启动。
一旦开始执行，该任务通常会进一步启动其他子任务。    

正如该类名称所示，大多数使用 ForkJoinTask 的程序只会用到 fork() 和 join() 这两个方法，或是使用它们的派生形式（例如 invokeAll）。
不过，该类还提供了若干高级方法和可扩展机制，以支持更复杂形式的 fork/join 处理。    



主要的协调机制有两个：
    fork()：安排任务的异步执行；(将fork方法的this对象由其他线程执行)
    join()：阻塞直到该任务的结果被计算出来。

理想情况下，任务的计算应当：
    避免使用 synchronized 方法或代码块；
    除了 join 其他任务或使用能与 fork/join 调度协作的同步器（如 Phaser）之外，尽量减少阻塞式同步；
    不应执行阻塞 I/O；
    最好只访问完全独立的变量（不与其他任务共享的状态）。
    
这些规范被“松散地”强制执行 ——
例如，不允许抛出受检异常（如 IOException），以此鼓励任务保持纯计算性质。
但任务仍可能抛出未检查异常（unchecked exceptions）。
当其他线程调用 join() 时，这些异常会被重新抛出。  



等待与结果提取
    join() 是等待任务完成并获取结果的主要方法。
还有其他变体：
    Future.get() 系列方法支持可中断或定时等待；
    invoke() 等价于 fork(); join()，但会尽量在当前线程启动执行；
    “quiet” 形式的方法不会提取结果或抛出异常 ——在批量执行任务、但希望延后处理结果或异常时非常有用；
    invokeAll()（有多个重载）执行最常见的并行调用形式：同时 fork 一组任务并等待它们全部完成。  
```

```text
doJoin 

目的：当某个线程要 join() 一个 ForkJoinTask 时，doJoin 是内部实现——它负责等待该任务完成并返回/传播其结果或异常。
要解决的问题：高效等待（避免忙等）、在工作窃取框架中“帮助”完成（避免无谓阻塞池内线程）、正确传播异常/取消信息、并处理中断与唤醒。

调用者有两类：
    ForkJoinWorkerThread（FJW）：池内工作线程。对它，框架会尝试“帮忙做点工作”（help-steal/help-join），以提升并行度和避免死锁/饥饿。
    外部线程（非 FJW）：例如主线程调用 task.join()。对它则通常直接阻塞、等待唤醒（使用 wait/notify 或 LockSupport.park/unpark），并不参与工作窃取。
    
关键字段与位
    volatile int status：存放多个控制位与完成信息（高 16 位用于控制位，低 16 位可做 tag）
    ForkJoinPool：包含帮助（helpJoin / helpQuiesce）及工作窃取队列机制。
    ForkJoinWorkerThread.workQueue：工作线程的本地双端队列（deque），用于 push/pop/steal。  
    
    
private int doJoin() {
    int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
    return (s = status) < 0 ? 
                     s :
                     (
                         ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
                                     (
                                         (w = (wt = (ForkJoinWorkerThread)t).workQueue).tryUnpush(this) && (s = doExec()) < 0 ? 
                                                    s :
                                                    wt.pool.awaitJoin(w, this, 0L)
                                     ) :
                                     externalAwaitDone()
                     );
}

等价于:
private int doJoin() {
    int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
    if ((s = status) < 0) {
        return s;
    }
    
    if((t = Thread.currentThread()) instanceof ForkJoinWorkerThread){
        if((w = (wt = (ForkJoinWorkerThread)t).workQueue).tryUnpush(this) && (s = doExec()) < 0){
            return s;
        } else {
            return wt.pool.awaitJoin(w, this, 0L);
        }
    } else {
        return externalAwaitDone();
    }
}

下面给出 doJoin(task) 的逻辑流程，分为“位检查 → 快速返回 → 助手尝试（help）→ 阻塞等待 → 唤醒与返回/抛异常”几个阶段。
1. 先做快速检查（fast-path）
2. 判断调用线程类型（FJW or external）
    如果当前线程是 ForkJoinWorkerThread（即工作线程,检查是否存在 ForkJoinPool 和 workQueue），进入工作线程路径；否则进入外部线程路径。
    
    
工作线程路径（池内线程的 doJoin）
   工作线程有能力并被鼓励帮助池中其它任务执行，以减少等待时间、避免活锁或线程饥饿.主要步骤：
1.尝试本地帮忙（help short-circuit）：
	如果任务的 fork 来自当前线程所在的工作队列（即任务是本线程推入的），工作线程通常会**先尝试自己执行该任务（直接 exec/call compute()）**而不是阻塞。
	或者，如果任务被窃取到其他队列，工作线程会尝试去帮助执行被窃取的任务（调用 ForkJoinPool.helpJoin / helpComplete 等帮助函数），以便加快完成。
2.自忙轮询 / 执行本线程队列中的任务：
	为了保持活跃，工作线程在等待目标任务完成时，会从自己的队列中弹出并执行其他任务（localPop），或尝试去窃取其他线程队列的任务（helpSteal）。
	这种“帮忙做其他工作”的行为有两重好处：提高 CPU 利用率，且可能直接或间接解决阻塞依赖（如果被等待的任务的子任务由这些任务之一完成的话）。
3.设置 SIGNAL（若需要）并最终阻塞：
	如果多次尝试帮助仍无果，或没有可做的工作，工作线程会把 SIGNAL 位设置到任务的 status 上，表明有线程在等待该任务。
	之后，工作线程会进入等待 —— 但对于 FJW，通常使用的是 ForkJoinPool 的管理等待（例如 workQueue.awaitJoin 或使用 LockSupport.park 与内部队列配合），不是简单的 synchronized/wait。等待时，线程会从活跃工作线程计数中临时转为“等待”状态，池可以据此决定是否启动其他补偿线程（或在 ManagedBlocker 场景下扩大线程）。
4.被唤醒 & 检查状态：
	当等待的任务被执行完毕（可能在别的线程中），完成方会设置任务 status 为 NORMAL / EXCEPTIONAL / CANCELLED，并检测 SIGNAL 位，若存在则触发唤醒机制（notifyAll 或 LockSupport.unpark 等），这样等待的线程被唤醒。
	工作线程回到检查 status，若任务完成则读取 outcome / exception 并返回或抛异常。

外部线程路径（非池内线程的 doJoin）        
   外部线程（例如主线程）没有参与窃取机制，也不适合执行池内任务（会破坏设计意图），所以行为更简单直接：

1.先做快速检查（如前述）。
2.如果未完成，设置 SIGNAL（有时这一步会在内部做）：
    为了能被完成方唤醒，调用方可能会把 SIGNAL 位 OR 到 status（原子 CAS 更新），表示有人在等待。
3.进入阻塞等待：
    通常外部线程会进入 synchronized(task) + task.wait() 的阻塞（在部分实现中），或使用 LockSupport.park 等。
    重要：JVM 的 monitor-inflation（监视器膨胀）机制和 notifyAll 的实现会被利用，以减少每个任务的额外开销（这是设计上的权衡）。
4.被唤醒并返回（或抛异常）：
    完成方在完成时会看见 SIGNAL，并调用 notifyAll（或针对 parked 线程 的 unpark），唤醒等待线程；外部线程检查结束状态并处理返回值或异常。
    
    
```