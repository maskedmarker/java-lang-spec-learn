# juc-forkjoin-ForkJoinTask


## javadoc描述

```text

```


## ForkJoinTask

```text
public abstract class ForkJoinTask<V> implements Future<V>, Serializable {
    volatile int status; // accessed directly by pool and workers
    
    // 预留给用户定义计算逻辑
    protected abstract boolean exec();
    
    // 该方法会被工作线程调用(必要时提交任务的外部线程执行)
    final int doExec() {
        int s; boolean completed;
        
        // status>=0表示任务需要被执行
        if ((s = status) >= 0) {
            try {
                completed = exec();
            } catch (Throwable rex) {
                return setExceptionalCompletion(rex);
            }
            
            if (completed)
                s = setCompletion(NORMAL);  // 任务正常执行完后,需要更新status状态,并唤醒挂起等待的线程
        }
        // 如果任务已经是终态,不需要被再次执行
        
        return s;
    }
    
    // ...
}
```

## 关键字段

```text
//  EXCEPTIONAL<CANCELLED<NORMAL<0 终态都是负数;SIGNAL是正数(表示有线程因为等待任务结果而挂起,此时需要被唤醒)
static final int DONE_MASK   = 0xf0000000;  // mask out non-completion bits       11110000_00000000_00000000_00000000
static final int NORMAL      = 0xf0000000;  // must be negative                   11110000_00000000_00000000_00000000
static final int CANCELLED   = 0xc0000000;  // must be < NORMAL                   11000000_00000000_00000000_00000000
static final int EXCEPTIONAL = 0x80000000;  // must be < CANCELLED                10000000_00000000_00000000_00000000
static final int SIGNAL      = 0x00010000;  // must be >= 1 << 16                 00000000_00000001_00000000_00000000 
static final int SMASK       = 0x0000ffff;  // short bits for tags                00000000_00000000_11111111_11111111  


// 任务状态
volatile int status; // accessed directly by pool and workers  初始值为0; status<0表示任务已结束处于终态; status>=0任务需要被执行
```

## 执行链路


## fork

向工作队列中提交当前任务(this)

```text
public final ForkJoinTask<V> fork() {
    Thread t;
    if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
        ((ForkJoinWorkerThread)t).workQueue.push(this);  // 提交到当前工作线程所在的工作队列
    else
        ForkJoinPool.common.externalPush(this);         // 通常fork发生在ForkJoinTask.exec()方法中,这里给了兜底保护.
    return this;
}
```

## join

```text
public final V join() {
    int s;
    if ((s = doJoin() & DONE_MASK) != NORMAL)
        reportException(s);
    return getRawResult();
}
```

## doJoin

```text
private int doJoin() {
    int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
    
    return (s = status) < 0 ? s :
        ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
        (w = (wt = (ForkJoinWorkerThread)t).workQueue).tryUnpush(this) && (s = doExec()) < 0 ? s :
        wt.pool.awaitJoin(w, this, 0L) :
        externalAwaitDone();
}

原代码不便于解读,转换为等价体
private int doJoin() {
    int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
    
    // status为负数,表示任务已结束处于终态,不用等待直接返回
    if((s = status) < 0) {
        return s;
    } else {
        if((t = Thread.currentThread()) instanceof ForkJoinWorkerThread){
            if((w = (wt = (ForkJoinWorkerThread)t).workQueue).tryUnpush(this) && ((s = doExec()) < 0)){
                return s;
            } else {
                return wt.pool.awaitJoin(w, this, 0L);
            }
        } else{
            return externalAwaitDone();
        }
    }
}
```

## externalAwaitDone

💯需要先确认如何被唤醒,该如何实现等待.反之亦然.

```text
private int externalAwaitDone() {
    // 先忽视CountedCompleter, 
    int s = ((this instanceof CountedCompleter) ? // try helping
             ForkJoinPool.common.externalHelpComplete(
                 (CountedCompleter<?>)this, 0) :
             ForkJoinPool.common.tryExternalUnpush(this) ? doExec() : 0);  // 如果当前任务位于(top-1),则从工作队列移除,并由当前线程直接执行
    
    // 如果已经是终态直接返回;非终态才需要等待
    if (s >= 0 && (s = status) >= 0) {
        boolean interrupted = false;
        do {
            if (U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {       // 先设置16th-bit,表示有线程需要被唤醒,再挂起等待
                synchronized (this) {
                    if (status >= 0) {                                    // 防止cas-signal后,setCompletion先获得锁,所以要二次检查status
                        try {
                            wait(0L);
                        } catch (InterruptedException ie) {
                            interrupted = true;
                        }
                    }
                    else
                        notifyAll();                                       // 如果setCompletion先获得锁设置了终态,唤醒挂起等待的线程 (setCompletion中会notifyAll,这里为什么还要再notifyAll???)
                }
            }
        } while ((s = status) >= 0);
        
        if (interrupted)
            Thread.currentThread().interrupt();
    }
    
    return s;
}

前面的CAS与获得monitor之间的间隙中,会发生setCompletion先获得了锁,将任务设置为已经结束(status<0),根本原因是signal与终态用的bit位不同.💯💯💯
而signal与终态用的bit位不同是为了减少对monitor的使用,增加对volatile-statue的依赖.
```

## Future方法

### cancel

```text
public boolean cancel(boolean mayInterruptIfRunning) {
    return (setCompletion(CANCELLED) & DONE_MASK) == CANCELLED;
}
```

### setCompletion

设置终态,并唤醒挂起等待的线程

```text
private int setCompletion(int completion) {
    for (int s;;) {
        if ((s = status) < 0)   // 终态都是负数,status不能再修改了
            return s;
        
        // 此时status>=0  cas原子操作status;如果cas失败则有并发操作,需要重试
        if (U.compareAndSwapInt(this, STATUS, s, s | completion)) {
            if ((s >>> 16) != 0)                                         // 16th-bit被设置了,表明有线程因为等待结果而挂起
                synchronized (this) { notifyAll(); }
            return completion;
        }
    }
}
```

### get(no-timeout)

```text
public final V get() throws InterruptedException, ExecutionException {
    int s = (Thread.currentThread() instanceof ForkJoinWorkerThread) ?
              doJoin() :                            // 如果当前线程是内部线程,就执行类似ForkJoinTask.join()的逻辑
              externalInterruptibleAwaitDone();     // 如果当前线程是外部线程
    
    Throwable ex;
    if ((s &= DONE_MASK) == CANCELLED)
        throw new CancellationException();
    
    if (s == EXCEPTIONAL && (ex = getThrowableException()) != null)
        throw new ExecutionException(ex);
    
    return getRawResult();
}
```

### get(timeout)

```text
public final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    int s;
    long nanos = unit.toNanos(timeout);
    if (Thread.interrupted())
        throw new InterruptedException();
    
    // 如果任务处于非终态,需要等待到终态    
    if ((s = status) >= 0 && nanos > 0L) {
        long d = System.nanoTime() + nanos;
        long deadline = (d == 0L) ? 1L : d; // avoid 0
        Thread t = Thread.currentThread();
        
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
            s = wt.pool.awaitJoin(wt.workQueue, this, deadline);
        }
        else if ((s = ((this instanceof CountedCompleter) ?
                       ForkJoinPool.common.externalHelpComplete((CountedCompleter<?>)this, 0) :
                       ForkJoinPool.common.tryExternalUnpush(this) ?
                       doExec() : 0)) >= 0) {
            long ns, ms; // measure in nanosecs, but wait in millisecs
            while ((s = status) >= 0 && (ns = deadline - System.nanoTime()) > 0L) {
                if ((ms = TimeUnit.NANOSECONDS.toMillis(ns)) > 0L &&
                    U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                    synchronized (this) {
                        if (status >= 0)
                            wait(ms); // OK to throw InterruptedException
                        else
                            notifyAll();
                    }
                }
            }
        }
    }
    
    // 此时任务已经是终态了,如果之前读到的是非终态,需要读取最新的状态
    if (s >= 0)
        s = status;
    
    // 如果终态是正常则直接返回结果;否则需要抛出对应的异常
    if ((s &= DONE_MASK) != NORMAL) {
        Throwable ex;
        if (s == CANCELLED)
            throw new CancellationException();
        if (s != EXCEPTIONAL)
            throw new TimeoutException();
        if ((ex = getThrowableException()) != null)
            throw new ExecutionException(ex);
    }
    
    return getRawResult();
}
```