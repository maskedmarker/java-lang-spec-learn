# juc-AbstractExecutorService


## ExecutorService

```text
Executor过于抽象,仅仅定义了一个方法execute(runnable).
如果用户想要知道任务的执行结果,还要用户实现查看任务执行结果的功能.这类需求是大量存在的.
同时Executor也没有定义统一的管理线程池的接口(Executor内部需要线程池支撑).
于是就有了ExecutorService.

submit         -> 支持提交不同类型的任务
invokeAll      -> 提交任务并等待任务完成
shutdown       -> 发起关闭线程池的流程
shutdownNow    -> 立即关闭线程池
```

## AbstractExecutorService

AbstractExecutorService将ExecutorService需要实现的共性的基础功能都实现了,最核心的execute方法留给子类来实现.

```text
实现的共性的基础功能:

// 薄薄的封装层
submit(需要newTaskFor和execute支持)    // 支持用户提交callable/runnable
newTaskFor(callable/runnable)        // 将用户提交的任务统一封装成RunnableFuture

// 公用实现
invokeAny
invokeAll
```

```text
public abstract class AbstractExecutorService implements ExecutorService {
    // 留给子类来实现核心方法
    // Executes the given command at some time in the future. The command may execute in a new thread, in a pooled thread, or in the calling thread
    abstract void execute(Runnable command); // command指的是待执行的任务
}
```
execute方法的javadoc中"at some time in the future",并结合"or in the calling thread",execute方法的语义并不一定是异步执行command的,只是说如果提交的command没有被拒绝则command会被执行.


## doInvokeAny

Executes the given tasks, returning the result of one that has completed successfully.
那些没来得及完成的会被cancel.

```text
// 注意这个方法是被线程池外的外部线程调用.

private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos) throws InterruptedException, ExecutionException, TimeoutException {
    if (tasks == null)
        throw new NullPointerException();
    int ntasks = tasks.size(); // 记录待提交的任务数
    if (ntasks == 0)
        throw new IllegalArgumentException();
    
    ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
    
    // 为了便于观察已提交的任务是否完成,使用了ExecutorCompletionService(ExecutorCompletionService内部需要Executor支持,所以就借用了当前线程池)
    ExecutorCompletionService<T> ecs = new ExecutorCompletionService<T>(this);

    // For efficiency, especially in executors with limited parallelism, check to see if previously submitted tasks are done before submitting more of them. 
    // This interleaving plus the exception mechanics account for messiness of main loop.(这种交叉指的是提交新任务之前,先检查之前提交的任务是否已经完成)

    try {
        // Record exceptions so that if we fail to obtain any result, we can throw the last exception we got.
        ExecutionException ee = null;
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        Iterator<? extends Callable<T>> it = tasks.iterator();

        // Start one task for sure; the rest incrementally
        futures.add(ecs.submit(it.next()));
        --ntasks;
        int active = 1; // 记录当前正在执行的任务数(在ecs.poll()获取到后才算作已完成)
        
        // 外部线程通过循环来等待任务的执行结果
        for (;;) {
            // 外部线程通过ecs.poll()检查之前提交到线程池的任务是否有新已经完成的
            Future<T> f = ecs.poll();
            if (f == null) { // 如果没有新的已完成任务
                if (ntasks > 0) { // 如果还有未提交的任务,则再提交一个新任务(优先级最高,外部线程优先通过增加新任务而非等待来尽早获取到结果.因为无法预估任务的执行时间,能做的就是更充分利用cpu的多核)
                    futures.add(ecs.submit(it.next()));
                    --ntasks; // 每提交一个任务,意味着待提交的任务数就少一个
                    ++active; // 每提交一个任务,正在执行的任务数就增加一个
                }
                else if (active == 0) // 如果任务都已提交,且已提交的任务都已完成,没有更多的办法,只能break无线循环,在后面抛出异常
                    break;
                else if (timed) { // 如果任务都已提交,还有任务没有没完成,就有限时间内等待队列最前的那个任务
                    f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                    if (f == null)
                        throw new TimeoutException();
                    nanos = deadline - System.nanoTime();
                }
                else
                    f = ecs.take(); // 如果任务都已提交,还有任务没有没完成,就无限等待队列最前的那个任务
            }
            if (f != null) { // 如果有新的已完成的任务
                --active; // ecs.poll()获取都是已完成的任务,每获取一个就意味着又一个任务已完成
                try {
                    return f.get(); // 任务执行无异常,则将最靠前的任务结果返回
                } catch (ExecutionException eex) {
                    ee = eex; // 如果任务执行结果是异常则先记录一下异常继续循环
                } catch (RuntimeException rex) {
                    ee = new ExecutionException(rex); // 如果任务执行结果是异常则先记录一下异常继续循环
                }
            }
        }

        if (ee == null)
            ee = new ExecutionException();
        throw ee;

    } finally {
        for (int i = 0, size = futures.size(); i < size; i++)
            futures.get(i).cancel(true);    // cancel那些没来得及完成的
    }
}
```

```text
doInvokeAny方法支持同时提交多个任务,只要返回一个任务成功完成的结果.
从逻辑上来讲,只要有一个完成的任务就不用向线程池再提交更多的任务.所以先提交一个任务,然后查看一下任务是否已完成,如果完成就提前结束方法,如果没有任务完成就接着继续提交(防止任务执行时间长短不一),如此循环直到有任务完成了就停止提交,或者都提交完成了就等最先完成的任务.
这样渐进式地提交任务,不过多浪费系统资源,降低上下文切换.
```


## invokeAll

Executes the given tasks, returning a list of Futures holding their status and results when all complete. 
Future.isDone is true for each element of the returned list.
当提交的所有任务都已经完成(正常完成/发生异常/被取消)时,才会从invokeAll方法返回.(可能发生无穷等待,如果想要等待有限时间,就需要使用带timeout的invokeAll版本)

```text
public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    if (tasks == null)
        throw new NullPointerException();
    ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
        for (Callable<T> t : tasks) {
            RunnableFuture<T> f = newTaskFor(t);
            futures.add(f);
            // 外部线程将所有的任务都提交到线程池中来异步执行
            execute(f);
        }
        for (int i = 0, size = futures.size(); i < size; i++) {
            Future<T> f = futures.get(i);
            // 通过get()方法来"盲目"地等待任务完成.这里的"盲目"并不盲目,多个任务的总耗时并不会因为外部线程精挑细选调用get方法而缩短,也就意味着外部线程无论什么顺序调用get方法,最终耗时是一样的.
            if (!f.isDone()) {
                try {
                    f.get();
                } catch (CancellationException ignore) {
                } catch (ExecutionException ignore) {
                }
            }
        }
        done = true;
        return futures;
    } finally {
        if (!done)
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);
    }
}
```

## invokeAll(timeout)

Executes the given tasks, returning a list of Futures holding their status and results when all complete or the timeout expires, whichever happens first.

```text
timeout指的是从任务提交开始计时,等待任务完成的最大时间

public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    if (tasks == null)
        throw new NullPointerException();
    long nanos = unit.toNanos(timeout);
    ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
        for (Callable<T> t : tasks)
            futures.add(newTaskFor(t));
        // 防止前面newTaskFor耗时多,从外部线程开始提交计时
        final long deadline = System.nanoTime() + nanos;
        final int size = futures.size();

        // Interleave time checks and calls to execute in case executor doesn't have any/much parallelism.
        for (int i = 0; i < size; i++) {
            execute((Runnable)futures.get(i));
            nanos = deadline - System.nanoTime();
            if (nanos <= 0L) // 主要是防止cpu的core过少,通过os时间切片来实现多线程导致execute和此行代码是2个时间片
                return futures;
        }

        for (int i = 0; i < size; i++) {
            Future<T> f = futures.get(i);
            if (!f.isDone()) {
                if (nanos <= 0L)
                    return futures;
                try {
                    f.get(nanos, TimeUnit.NANOSECONDS);
                } catch (CancellationException ignore) {
                } catch (ExecutionException ignore) {
                } catch (TimeoutException toe) {
                    return futures;
                }
                nanos = deadline - System.nanoTime();
            }
        }
        done = true;
        return futures;
    } finally {
        if (!done)
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);
    }
}
```