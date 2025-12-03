# juc-future-FutureTask

## FutureTask

```text
public interface RunnableFuture<V> extends Runnable, Future<V> {
    // Sets this Future to the result of its computation unless it has been cancelled.
    void run();
}

extends Runnable:
计算(Runnable)不需要入参也无返参,所需全部变量来自于自身初始状态
extends Future:
计算的side-effect会为Future设置结果
RunnableFuture<V> extends Runnable, Future<V>:
表示一个异步计算,该异步计算的结果可以通过get()方法获取
```

```text
public class FutureTask<V> implements RunnableFuture<V> {
    // 表示该计算的状态
    private volatile int state;
    
    /** 构造函数的入参最终都要转化为等价的Callable, Callable的call()方法的返回值就是异步计算的结果值 */
    private Callable<V> callable;
    
    // 记录执行该计算任务的线程
    private volatile Thread runner;
    // Treiber stack of waiting threads  (Treiber Stack实现了lock-free Last-In-First-Out stack semantics,它使用原子操作CAS来修改栈顶指针, 操作只有push() and pop())
    private volatile WaitNode waiters;
    
    // Callable:定义计算过程且计算结果作为异步计算的值
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;
    }
    
    // 构造FutureTask时已经定义好了异步计算的结果值result,但是想要获取到该结果值,需要等到异步计算执行完才能获取到
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;
    }    
}
```

```text
private static final int NEW          = 0;
private static final int COMPLETING   = 1;
private static final int NORMAL       = 2;
private static final int EXCEPTIONAL  = 3;
private static final int CANCELLED    = 4;
private static final int INTERRUPTING = 5;
private static final int INTERRUPTED  = 6;

The run state of this task, initially NEW. 
The run state transitions to a terminal state only in methods set, setException, and cancel. (只有set/setException/cancel这3个方法能更改state.)
During completion, state may take on transient values of COMPLETING (while outcome is being set) or INTERRUPTING (only while interrupting the runner to satisfy a cancel(true)). 
Transitions from these intermediate to final states use cheaper ordered/lazy writes because values are unique and cannot be further modified. 
Possible state transitions: 
NEW -> COMPLETING -> NORMAL/EXCEPTIONAL 
NEW -> CANCELLED (COMPLETING时就不能取消了,将计算看作是原子操作;INTERRUPTING时就不能取消了,将中断看作是原子操作)
NEW -> INTERRUPTING -> INTERRUPTED(计算/中断看作原子操作,计算时和取消时都不能中断)


设置outcome和state是2个独立的变量无法一个CAS完成操作,所以使用了COMPLETING过度态来先设置state,再设置outcome,再使用NORMAL/EXCEPTIONAL终态设置state,这样state的终态永远发生在outcome之后,在read时先判断state再取值outcome.整个过程类似与事务的二阶段提交.

总结:计算/取消/中断都被看作是一个个原子操作

--------------------

// 注意:取消包括主动取消和被动中断而取消
public boolean isCancelled() {
    // 除完成计算(正常/异常)外,都是取消了(主动取消和被动中断而取消,即CANCELLED/INTERRUPTING+INTERRUPTED)
    return state >= CANCELLED;
}

public boolean isDone() {
    // 任务完成包括 正常/异常/取消(即COMPLETING+NORMAL/EXCEPTIONAL CANCELLED INTERRUPTING+INTERRUPTED)
    return state != NEW;
}

protected void set(V v) {
    if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { //只有初始状态NEW才能更新状态并设置outcome
        outcome = v;
        UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
        finishCompletion();
    }
}

protected void setException(Throwable t) {
    if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { //只有初始状态NEW才能更新状态并设置outcome
        outcome = t;
        UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
        finishCompletion();
    }
}    

// 如果允许主动取消(中断,则走INTERRUPTING->INTERRUPTED 如果不允许只能CANCELLED
public boolean cancel(boolean mayInterruptIfRunning) {
    // 只有还未开始计算才允许取消操作
    if (!(state == NEW && UNSAFE.compareAndSwapInt(this, stateOffset, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) //cancel和run同时执行时,会同时看到NEW状态并同时继续往下执行
        return false;
    try {    // in case call to interrupt throws exception
        if (mayInterruptIfRunning) {
            try {
                Thread t = runner;
                if (t != null)
                    t.interrupt(); // 存在cancel和run同时执行,run会同时看到NEW状态并设置了runner
            } finally { // final state
                UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
            }
        }
    } finally {
        finishCompletion();
    }
    return true;
}

// 注意执行run时,先设置runner,然后执行计算过程,再然后才是开始设置状态
// run与cancel的竞争仅在对state的write处,run即使setXX失败也会存在callable已经执行完,即此时callable的计算结果被ignore.
public void run() {
    if (state != NEW || // 如果state已经不是NEW了,肯定已经done或cancel,就不用再run了
        !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())) // 通过CAS设置runner来实现只有一个线程执行,并不能阻止cancel操作.(无法同时CAS runner和state!!!!!!)
        return;
    try {
        Callable<V> c = callable;
        if (c != null && state == NEW) { // cancel和run同时执行时,会同时看到NEW状态并同时继续往下执行,run往下执行,cancel将state设置为INTERRUPTING或CANCELLED
            V result;
            boolean ran;
            try {
                result = c.call(); // 即使callable中会处理cancel的中断信号抛出异常也无法完成setException(因为此时state已经是INTERRUPTING/INTERRUPTED)
                ran = true;
            } catch (Throwable ex) {
                result = null;
                ran = false;
                setException(ex); //只有此时state是NEW才能设置state和异常结果,如果之前已经被cancel则无法设置
            }
            if (ran)
                set(result); //只有此时state是NEW才能设置state和异正常结果,如果之前已经被cancel则无法设置
        }
    } finally {
        // runner must be non-null until state is settled to prevent concurrent calls to run()
        // 这里设置null是为了方便gc
        runner = null;
        // state must be re-read after nulling runner to prevent leaked interrupts (此时也并不是真真正正处理cancel的中断信号,仅仅是为了等INTERRUPTING的终态)
        int s = state;
        if (s >= INTERRUPTING)
            handlePossibleCancellationInterrupt(s); //该方法仅仅时等待INTERRUPTING过度状态转变为INTERRUPTED终态的瞬时过渡期
    }
}

为什么FutureTask允许存在state==INTERRUPTED, 且callable已经执行过了?
The Java Future contract explicitly says:
“Cancellation is a best-effort attempt to stop the task.”
If the task has already started, interruption is attempted but cannot guarantee that the task will stop.
That means the system never guarantees atomicity between “not started” and “running”.
The cancellation only controls the observable completion state, not the physical execution lifecycle.That’s why state can be INTERRUPTED even though the callable body executed (partially or fully).
FutureTask prioritizes external consistency over internal termination.
在 FutureTask 的设计和运行机制中，它更注重保证外部对它的调用、状态感知等方面的一致性和正确性，而不是单纯地关注内部任务是否已经终止。

public V get() throws InterruptedException, ExecutionException {
    int s = state;
    if (s <= COMPLETING)
        s = awaitDone(false, 0L);
    return report(s);
}
private V report(int s) throws ExecutionException {
    Object x = outcome;
    if (s == NORMAL)
        return (V)x;
    if (s >= CANCELLED)
        throw new CancellationException();
    throw new ExecutionException((Throwable)x);
}

public V get(long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
    if (unit == null)
        throw new NullPointerException();
    int s = state;
    if (s <= COMPLETING &&
        (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
        throw new TimeoutException();
    return report(s);
}
```