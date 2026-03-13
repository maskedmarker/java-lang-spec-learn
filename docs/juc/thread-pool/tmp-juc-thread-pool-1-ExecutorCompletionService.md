# juc-ExecutorCompletionService

相比Executor,ExecutorCompletionService额外提供查询任务执行结果的功能(只能查一次)

## CompletionService

CompletionService可以看作是对Executor的扩展.
CompletionService在提交任务时可以返回future(后续可以查看任务是否完成).
CompletionService还提供了任务执行情况的闻讯服务(take),用户不用保存提交时返回的future.

```text
public interface CompletionService<V> {
    
    // 提交需要执行的任务
    Future<V> submit(Callable<V> task);
    
    // 获取已经完成的任务(非阻塞/超时版本省略)
    Future<V> take() throws InterruptedException;  // Retrieves and removes the Future representing the next completed task
}
```

## ExecutorCompletionService

CompletionService相比Executor就多个take功能,CompletionService通过依靠Executor来实现自我功能是一个很好的捷径.
而且Executor的实现类非常多,所以就有了CompletionService主要实现类ExecutorCompletionService.

```text
public class ExecutorCompletionService<V> implements CompletionService<V> {

    // 依赖Executor来完成submit功能
    private final Executor executor;
    private final AbstractExecutorService aes;  // 无关紧要
    
    // 为实现take功能,就要保留任务的执行状态
    private final BlockingQueue<Future<V>> completionQueue;
}
```

```text
// 为了方便获取任务的执行状态,需要将用户提交的任务封装起来

private class QueueingFuture extends FutureTask<Void> {
    
    // 这是用户提交的任务对象
    private final Future<V> task;
    
    QueueingFuture(RunnableFuture<V> task) {
        super(task, null);
        this.task = task;
    }
    
    // FutureTask任务完成后调用这个预留给子类的回调方法
    protected void done() {
        completionQueue.add(task);   // 将完成的任务保存起来
    }
}
```

```text
public ExecutorCompletionService(Executor executor) {
    if (executor == null)
        throw new NullPointerException();
    this.executor = executor;
    this.aes = (executor instanceof AbstractExecutorService) ?
        (AbstractExecutorService) executor : null;
    this.completionQueue = new LinkedBlockingQueue<Future<V>>();    // 默认使用长度无限制的LinkedBlockingQueue,用户如果不调用take()方法,会造成OOM
}

public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
    if (executor == null || completionQueue == null)
        throw new NullPointerException();
    this.executor = executor;
    this.aes = (executor instanceof AbstractExecutorService) ? (AbstractExecutorService) executor : null;
    this.completionQueue = completionQueue;                                                                  // 可以自定义completionQueue.如果长度有限,那么当队列满的时候,QueueingFuture.done()方法又该如何处理?是阻塞还是报错而达到告警
}
```