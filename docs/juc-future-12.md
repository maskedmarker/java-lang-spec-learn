# juc-future

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
    // Treiber stack of waiting threads  (Treiber Stack 是一种著名的无锁（lock-free）栈（stack）实现,它使用原子操作 CAS（Compare-And-Swap）来修改栈顶指针)
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
The run state transitions to a terminal state only in methods set, setException, and cancel. 
During completion, state may take on transient values of COMPLETING (while outcome is being set) or INTERRUPTING (only while interrupting the runner to satisfy a cancel(true)). 
Transitions from these intermediate to final states use cheaper ordered/lazy writes because values are unique and cannot be further modified. 
Possible state transitions: 
NEW -> COMPLETING -> NORMAL 
NEW -> COMPLETING -> EXCEPTIONAL 
NEW -> CANCELLED 
NEW -> INTERRUPTING -> INTERRUPTED


set, setException, and cancel能改动FutureTask的状态
COMPLETING/INTERRUPTING都是瞬时态,然后必然到终态.

    protected void set(V v) {
        if (STATE.compareAndSet(this, NEW, COMPLETING)) {
            outcome = v;
            STATE.setRelease(this, NORMAL); // final state
            finishCompletion();
        }
    }

    protected void setException(Throwable t) {
        if (STATE.compareAndSet(this, NEW, COMPLETING)) {
            outcome = t;
            STATE.setRelease(this, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW && STATE.compareAndSet(this, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        try {
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally { // final state
                    STATE.setRelease(this, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }
    
因为COMPLETING/INTERRUPTING都是瞬时态,然后必然到终态. 所以
    public boolean isDone() {
        return state != NEW;
    }
```