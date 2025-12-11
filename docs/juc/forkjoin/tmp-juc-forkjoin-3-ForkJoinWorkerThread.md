# juc-forkjoin-ForkJoinWorkerThread


## javadoc描述

```text

```


## 关键字段

```text
public class ForkJoinWorkerThread extends Thread {

    final ForkJoinPool pool;                // the pool this thread works in
    final ForkJoinPool.WorkQueue workQueue; // 为工作线程分配的本地工作队列
    
    // ForkJoinWorkerThread通过工厂类ForkJoinWorkerThreadFactory创建.调用该构造函数
    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this); // 让ForkJoinPool为当前ForkJoinWorkerThread分配一个WorkQueue
    }
}
```

## 执行链路

```text
ForkJoinWorkerThreadFactory.newThread
    Thread.start
        ForkJoinWorkerThread.run
            ForkJoinPool.runWorker (线程的主循环)
                ForkJoinPool.scan
                WorkQueue.runTask
                ForkJoinPool.awaitWork
```

## run

```text
public void run() {
    if (workQueue.array == null) { // only run once
        Throwable exception = null;
        try {
            onStart();
            pool.runWorker(workQueue);   // 💯 核心的执行逻辑放在了ForkJoinPool.runWorker方法中,方便访问ForkJoinPool的各个变量
        } catch (Throwable ex) {
            exception = ex;
        } finally {
            try {
                onTermination(exception);
            } catch (Throwable ex) {
                if (exception == null)
                    exception = ex;
            } finally {
                pool.deregisterWorker(this, exception);
            }
        }
    }
}
```
