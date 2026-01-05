# juc-forkjoin-WorkQueue


Doug Lea将WorkQueue设置成了active-object,将ForkJoinWorkerThread隐藏在后面默默执行任务.
工作线程默默从工作队列获取任务,然后执行任务,然后再获取任务,如此循环.如果要新增任务,只需要将任务放入工作队列即可.

## javadoc描述

```text
Most operations occur within work-stealing queues (in nested class WorkQueue).  
These are special forms of Deques that support only three of the four possible end-operations -- push, pop, and poll (aka steal), 📌
under the further constraints that push and pop are called only from the owning thread (or, as extended here, under a lock), while poll may be called from other threads.  

The main differences ultimately stem from GC requirements that we null out taken slots as soon as we can, to maintain as small a footprint as possible even in programs generating huge numbers of tasks. 
To accomplish this, we shift the CAS arbitrating pop vs poll (steal) from being on the indices ("base" and "top") to the slots themselves.

(If you are unfamiliar with them, you probably want to read Herlihy and Shavit's book "The Art of Multiprocessor programming", chapter 16 describing these in more detail before proceeding.)  
The main work-stealing queue design is roughly similar to those in the papers "Dynamic Circular Work-Stealing Deque" by Chase and Lev, SPAA 2005(http://research.sun.com/scalable/pubs/index.html) and "Idempotent work stealing" by Michael, Saraswat, and Vechev, PPoPP 2009 (http://portal.acm.org/citation.cfm?id=1504186).
```

```text
Adding tasks then takes the form of a classic array push(task):
   q.array[q.top] = task; ++q.top;

(The actual code needs to null-check and size-check the array,properly fence the accesses, and possibly signal waitingworkers to start scanning -- see below.)  
Both a successful pop and poll mainly entail a CAS of a slot from non-null to null.

The pop operation (always performed by owner) is:
  if ((base != top) and
       (the task at top slot is not null) and
       (CAS slot to null))
          decrement top and return task;

And the poll operation (usually by a stealer) is
   if ((base != top) and
       (the task at base slot is not null) and
       (base has not changed) and
       (CAS slot to null))
          increment base and return task;
```


```text
WorkQueues are also used in a similar way for tasks submitted to the pool. 
We cannot mix these tasks in the same queues used by workers. Instead, we randomly associate submission queues with submitting threads, using a form of hashing.  
The ThreadLocalRandom probe value serves as a hash code for choosing existing queues, and may be randomly repositioned upon contention with other submitters.  
In essence, submitters act like workers except that they are restricted to executing local tasks that they submitted (or in the case of CountedCompleters, others with the same root task).  📌📌📌
Insertion of tasks in shared mode requires a lock (mainly to protect in the case of resizing) but we use only a simple spinlock (using field qlock), 
because submitters encountering a busy queue move on to try or create other queues -- they block only when creating and registering new queues. 
Additionally, "qlock" saturates to an unlockable value (-1) at shutdown. Unlocking still can be and is performed by cheaper ordered writes of "qlock" in successful cases, but uses CAS in unsuccessful cases.
```


## 关键字段

WorkQueue的字段是经过设置的,排列考虑缓存行(通常64字节)
这里打乱了顺序,是为了将重要的/联系紧密的字段放在一起,方便学习.

### WorkQueue

```text
static final class WorkQueue {

         
        // 核心数据字段
        volatile int base;         // index of next slot for poll (base is the index of the oldest element) (stealer线程操作)
        int top;                   // index of next slot for push (owner线程操作)
        ForkJoinTask<?>[] array;   // the elements (initially unallocated) (容纳任务, 使用方式是环形数组) 
        // 数组被循环使用核心是通过取模来实现的,top/base并不会被置0用于reset.push时top++,pop时top--,poll时base++.区间[base,top)就是有效的数据区. 理论上可以暂时容纳Integer.MAX_VALUE个待处理任务,在工程上足够了
        
        
        // 控制字段
        volatile int scanState;    // versioned, <0: inactive; odd:scanning (非负数表示active,正奇数表示scanning(初始值是正奇数);负数表示inactive,当inactive时scanState被当作自增版本号)  (看registerWorker方法中的解释)
        volatile int qlock;        // 1: locked, < 0: terminate; else 0     (scanState仅控制active/inactive,q控制terminate)
        
        
        final ForkJoinPool pool;   // the containing pool (may be null)
        final ForkJoinWorkerThread owner; // owning thread or null if shared
        volatile Thread parker;    // == owner during call to park; else null
        
        
        volatile ForkJoinTask<?> currentJoin;  // task being joined in awaitJoin
        volatile ForkJoinTask<?> currentSteal; // mainly used by helpStealer
        
        
        
        int config;                // pool index and mode (- MODE_MASK: 模式掩码 - OWNED: 是否为绑定线程的队列 - SHARED_QUEUE: 是否为共享队列)
        
        // 辅助字段
        int stackPred;             // pool stack (ctl) predecessor (ForkJoinPool.ctl的前值)
        int nsteals;               // number of steals (当nsteals<0即达到最大值时,将数据转移到ForkJoinPool.stealCounter)
        int hint;                  // randomization and stealer index hint (工作窃取相关的目标队列索引, 当本队列为空时,从此队列尝试窃取任务)
}
```

### scanState

```text
scanState (32-bit int)

+--------------------------------+----------------------------------------------------------------+-----------------------+
|   inactive-bit (1-bit)         |            sequence bits (30-bits)                             | scanning-bit (1-bit)  |
+--------------------------------+----------------------------------------------------------------+-----------------------+

                             31     30                                                          1                        0

not-scanning表示工作线程此时在执行工作任务逻辑(即执行ForkJoinTask.doExe()方法);scanning表示工作线程此时没有执行工作任务逻辑.

sequence-bits初始取自workQueues奇数索引的1st~16th的位序列,其后tryRelease和signalWorker都会每次自增(1<<16)  (Total-Count只有16bits,自增保持低16bit序列不变)
```

```text
WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner) {
    this.pool = pool;
    this.owner = owner;
    
    // 因为是循环数组,初始base/top指向数组的中心处
    base = top = INITIAL_QUEUE_CAPACITY >>> 1;
}
```

## 核心操作

核心操作push/pop/poll中pop/poll存在并发,通过cas来判定先后.


### growArray

```text
final ForkJoinTask<?>[] growArray() {
    ForkJoinTask<?>[] oldA = array;
    int size = oldA != null ? oldA.length << 1 : INITIAL_QUEUE_CAPACITY;   // 容量翻倍
    if (size > MAXIMUM_QUEUE_CAPACITY)
        throw new RejectedExecutionException("Queue capacity exceeded");
    
    int oldMask, t, b;
    ForkJoinTask<?>[] a = array = new ForkJoinTask<?>[size];
    if (oldA != null && (oldMask = oldA.length - 1) >= 0 &&
        (t = top) - (b = base) > 0) {                            // top=base时工作队列为空;top>base时工作队列不为空
        int mask = size - 1;
        
        do {
            ForkJoinTask<?> x;
            int oldj = ((b & oldMask) << ASHIFT) + ABASE;
            int j    = ((b &    mask) << ASHIFT) + ABASE;     // 由于容量翻倍,所以j==oldj
            x = (ForkJoinTask<?>)U.getObjectVolatile(oldA, oldj);
            if (x != null && U.compareAndSwapObject(oldA, oldj, x, null))   // 将旧工作队列的任务转移到新工作队列中,就队列中对应位置设置为null
                U.putObjectVolatile(a, j, x);
        } while (++b != t); // 保持b<=top-1
    }
    return a;
}

如下2个操作,会让与growArray同时发生的poll操作看到null值,需要poll操作注意
array = new ForkJoinTask<?>[size];
U.compareAndSwapObject(oldA, oldj, x, null)
```

### push

owner线程在循环数组的top处添加task

```text
final void push(ForkJoinTask<?> task) {
    ForkJoinTask<?>[] a; ForkJoinPool p;
    int b = base, s = top, n;      // 提取base/top快照,供后面使用
    
    if ((a = array) != null) {    // ignore if queue removed
        int m = a.length - 1;     // m is the bit-mask for index wrapping when the array length is a power of two. (array.size保证是2的指数)
        
        U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);    // 在array的top slot中添加task     | (m & s)即循环数组的索引值
        U.putOrderedInt(this, QTOP, s + 1);                          // top加一
        
        if ((n = s - b) <= 1) {     // If n <= 1 means the queue was empty or nearly empty before this push📌 在添加这个任务前,工作队列将近为空,发生空闲工作线程的概率比较大,需要尝试唤醒一下
            if ((p = pool) != null)
                p.signalWork(p.workQueues, this);
        }
        else if (n >= m)            // m = a.length - 1, n >= m means the queue is full (or nearly full)📌  每次任务push完成后,还要提前做好扩容操作  防止了(top&m)后的数组index大于较小的base的数组index
            growArray();     // 扩容不会改变原有任务的索引值
    }
}

备注: 
U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task)
    ABASE是数组array的基地址
    ASHIFT指的是数组元素占用的字节数
    (m & s)即循环数组的索引值    array.size保证是2的指数, (m & s)等价于(s % a.length)
    (m&s)<< ASHIFT 意图: multiply index i by the element size in bytes.
    ((m&s)<< ASHIFT)+ABASE 的值就是task在array中的index值对应的数组元素的地址 
```

### pop

owner线程在循环数组的top处移除task

Takes next task, if one exists, in LIFO order. Call only by owner in unshared queues.

```text
final ForkJoinTask<?> pop() {
    ForkJoinTask<?>[] a; ForkJoinTask<?> t; int m;
    
    if ((a = array) != null && (m = a.length - 1) >= 0) {
        for (int s; (s = top - 1) - base >= 0;) {
            
            long j = ((m & s) << ASHIFT) + ABASE;
            if ((t = (ForkJoinTask<?>)U.getObject(a, j)) == null)  // 如果stealer抢先获取任务,就放弃
                break;
            
            if (U.compareAndSwapObject(a, j, t, null)) { // 通过cas抢占top处任务  | 使用CAS防止并发的
                U.putOrderedInt(this, QTOP, s); // top减一
                return t;
            }
        }
    }
    
    return null;
}
```

### poll

stealer线程在循环数组的base处移除task

Takes next task, if one exists, in FIFO order.

```text
final ForkJoinTask<?> poll() {
    ForkJoinTask<?>[] a; int b; ForkJoinTask<?> t;
    
    while ((b = base) - top < 0 && (a = array) != null) {   // 当队列中还有数据    |获取base/array快照,读取本地top
        int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
        t = (ForkJoinTask<?>)U.getObjectVolatile(a, j);  // 获取base处的任务
        
        if (base == b) { // 并发的poll已经成功了,直接循环重试
            if (t != null) {  // 并发的growArray会导致t==null,如果遇到了重试即可
                if (U.compareAndSwapObject(a, j, t, null)) {   // 通过cas抢占base处任务
                    base = b + 1;  // base加一
                    return t;
                }
            }
            
            else if (b + 1 == top) // 最后一个任务被其他线程的pop/poll抢走了,直接放弃
                break;
        }
    }
    
    return null;
}
```
### tryUnpush

Pops the given task only if it is at the current top. (A shared version is available only via FJP.tryExternalUnpush)
如果入参t是最近压入的任务,即位于(top-1)处,则移除.

```text
final boolean tryUnpush(ForkJoinTask<?> t) {
    ForkJoinTask<?>[] a; int s;
    
    if ((a = array) != null && (s = top) != base &&
        U.compareAndSwapObject(a, (((a.length - 1) & --s) << ASHIFT) + ABASE, t, null)) {
        U.putOrderedInt(this, QTOP, s); // 移除(top-1)处任务后top--
        return true;
    }
    
    return false;
}
```

## 核心方法

### runTask

Executes the given task and any remaining local tasks.

本方法仅被ForkJoinPool.runWorker调用.
task被当作top-level,在fork-join中分治的假设框架下,task在执行的过程中会产生子任务,且子任务都被保存至本工作队列中.所以不仅要执行task还有把子任务也都执行了
当然,存在子任务被其他工作线程steal.

当工作线程执行steal-working时,scanState会被设置为奇数,表示scanning开始;
当工作线程执行任务的doExec方法时,scanState会被设置为偶数,表示scanning结束;


```text
final void runTask(ForkJoinTask<?> task) {
    if (task != null) {
        scanState &= ~SCANNING; // WorkQueue.scanState设置为偶数,表示scanning结束(scanState的初始值就是奇数,表示scanning)
        
        // 执行入参指向的任务
        (currentSteal = task).doExec();
        U.putOrderedObject(this, QCURRENTSTEAL, null); // release for GC 执行完任务后清空currentSteal
        
        // 这里体现了fork-join中分治的假设: task被当作top-level,在执行该task的doExec方法时会产生子任务,且子任务都被保存至本工作队列中(此时工作队列被当作stack)
        // 将task产生的子任务也都执行了
        execLocalTasks();
        
        ForkJoinWorkerThread thread = owner;
        if (++nsteals < 0)      
            transferStealCount(pool); // 转移WorkQueue.nsteals数值到ForkJoinPool.stealCounter
            
        scanState |= SCANNING;   // WorkQueue.scanState设置为奇数,表示scanning开始
        
        if (thread != null)
            thread.afterTopLevelExec(); // 无关紧要的方法
    }
}
```

### execLocalTasks

不去steal,只执行本WorkQueue中的任务,默认使用pop操作即LIFO,这也更好利用cpu的缓存

```text
final void execLocalTasks() {
    int b = base, m, s;
    ForkJoinTask<?>[] a = array;
    
    // top-1指向最后插入的任务,base<=top-1表示本地还有待执行的任务
    if (b - (s = top - 1) <= 0 && a != null &&
        (m = a.length - 1) >= 0) {
        
        if ((config & FIFO_QUEUE) == 0) {  // 根据config配置来决定FIFO还是LIFO
            
            // pop操作:从top-1处移除任务,然后top--,直到本地任务执行完毕
            for (ForkJoinTask<?> t;;) {
                
                if ((t = (ForkJoinTask<?>)U.getAndSetObject(a, ((m & s) << ASHIFT) + ABASE, null)) == null)
                    break;     // 如果cas获取的是null,证明本地任务已经没有了
                U.putOrderedInt(this, QTOP, s); 
                
                t.doExec();   // 执行任务
                
                if (base - (s = top - 1) > 0) // 循环invariant, [base,top)有任务
                    break;
            }
        }
        else
            pollAndExecAll(); //LIFO地执行任务
    }
}

---------------------------------------------------------------------------

// pop操作从top处取任务以实现的是LIFO,poll操作从base处取任务以实现的是FIFO
final void pollAndExecAll() {
    for (ForkJoinTask<?> t; (t = poll()) != null;)
        t.doExec();
}
```

### tryRemoveAndExec


```text
final boolean tryRemoveAndExec(ForkJoinTask<?> task) {
    ForkJoinTask<?>[] a; int m, s, b, n;
    if ((a = array) != null && (m = a.length - 1) >= 0 && task != null) {
        
        while ((n = (s = top) - (b = base)) > 0) {
            for (ForkJoinTask<?> t;;) {
                // 从top往base遍历
                long j = ((--s & m) << ASHIFT) + ABASE;
                if ((t = (ForkJoinTask<?>)U.getObject(a, j)) == null)    // 读取null证明有并发操作(pop/poll-steal/扩容)
                    return s + 1 == top;     // shorter than expected
                
                // 只操作top处的任务
                else if (t == task) {
                    boolean removed = false;
                    if (s + 1 == top) {      // pop
                        if (U.compareAndSwapObject(a, j, task, null)) {
                            U.putOrderedInt(this, QTOP, s);
                            removed = true;
                        }
                    }
                    else if (base == b)      // 在base处替换一个空任务,防止并发问题
                        removed = U.compareAndSwapObject(a, j, task, new EmptyTask());
                    if (removed)
                        task.doExec();
                    break;
                }
                else if (t.status < 0 && s + 1 == top) {                // 将top处的终态任务从工作队列移除(因为是循环数组,这样可以加快对象gc)
                    if (U.compareAndSwapObject(a, j, t, null))
                        U.putOrderedInt(this, QTOP, s);
                    break;                                              // 每次改动队列元素都要重新跑外层的while循环
                }
                if (--n == 0)
                    return false;
            }
            if (task.status < 0)
                return false;
        }
    }
    return true;
}
```

### awaitWork

Possibly blocks worker w waiting for a task to steal, or returns false if the worker should terminate. 
If inactivating w has caused the pool to become quiescent, checks for pool termination, and, so long as this is not the only worker, waits for up to a given duration. 
On timeout, if ctl has not changed, terminates the worker, which will in turn wake up another worker to possibly repeat this process.
Params:
        w – the calling worker 
        r – a random seed (for spins)
Returns: false if the worker should terminate


```text
private boolean awaitWork(WorkQueue w, int r) {
    if (w == null || w.qlock < 0)                 // WorkQueue.qlock<0表示terminate
        return false;
    
    for (int pred = w.stackPred, spins = SPINS, ss;;) {
        if ((ss = w.scanState) >= 0)
            break;
        else if (spins > 0) {
            r ^= r << 6; r ^= r >>> 21; r ^= r << 7;
            if (r >= 0 && --spins == 0) {         // randomize spins
                WorkQueue v; WorkQueue[] ws; int s, j; AtomicLong sc;
                if (pred != 0 && (ws = workQueues) != null &&
                    (j = pred & SMASK) < ws.length &&
                    (v = ws[j]) != null &&        // see if pred parking
                    (v.parker == null || v.scanState >= 0))
                    spins = SPINS;                // continue spinning
            }
        }
        else if (w.qlock < 0)                     // recheck after spins
            return false;
        else if (!Thread.interrupted()) {
            long c, prevctl, parkTime, deadline;
            int ac = (int)((c = ctl) >> AC_SHIFT) + (config & SMASK);
            if ((ac <= 0 && tryTerminate(false, false)) ||
                (runState & STOP) != 0)           // pool terminating
                return false;
            if (ac <= 0 && ss == (int)c) {        // is last waiter
                prevctl = (UC_MASK & (c + AC_UNIT)) | (SP_MASK & pred);
                int t = (short)(c >>> TC_SHIFT);  // shrink excess spares
                if (t > 2 && U.compareAndSwapLong(this, CTL, c, prevctl))
                    return false;                 // else use timed wait
                parkTime = IDLE_TIMEOUT * ((t >= 0) ? 1 : 1 - t);
                deadline = System.nanoTime() + parkTime - TIMEOUT_SLOP;
            }
            else
                prevctl = parkTime = deadline = 0L;
            Thread wt = Thread.currentThread();
            U.putObject(wt, PARKBLOCKER, this);   // emulate LockSupport
            w.parker = wt;
            if (w.scanState < 0 && ctl == c)      // recheck before park
                U.park(false, parkTime);
            U.putOrderedObject(w, QPARKER, null);
            U.putObject(wt, PARKBLOCKER, null);
            if (w.scanState >= 0)
                break;
            if (parkTime != 0L && ctl == c &&
                deadline - System.nanoTime() <= 0L &&
                U.compareAndSwapLong(this, CTL, c, prevctl))
                return false;                     // shrink pool
        }
    }
    return true;
}
```

### popCC

如果工作队列(top-1)处的任务是task的子任务,则将(top-1)处的任务弹出,否则返回null;

备注: fork的新任务都放在当前工作线程的工作任务队列中

```text
final CountedCompleter<?> popCC(CountedCompleter<?> task, int mode) {
    // 用来保存快照(这是ForkJoinPool中典型的 optimistic + snapshot 模式)
    int s; ForkJoinTask<?>[] a; Object o;
    
    if (base - (s = top) < 0 && (a = array) != null) {
        long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;  // 获取(top-1)处的任务
        if ((o = U.getObjectVolatile(a, j)) != null && (o instanceof CountedCompleter)) {  // 如果(top-1)处的任务是CountedCompleter
            CountedCompleter<?> t = (CountedCompleter<?>)o;
            
            // 以(top-1)处的任务作为CountedCompleter的父子链的起点,往上查找来判断(top-1)处的任务是否task的子任务:如果(top-1)处的任务是task的子任务,则将(top-1)处的任务弹出
            for (CountedCompleter<?> r = t;;) {
                if (r == task) {
                    if (mode < 0) { // shared mode
                        if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                            if (top == s && array == a && U.compareAndSwapObject(a, j, t, null)) {
                                U.putOrderedInt(this, QTOP, s - 1);
                                U.putOrderedInt(this, QLOCK, 0);
                                return t;
                            }
                            U.compareAndSwapInt(this, QLOCK, 1, 0);
                        }
                    }
                    else if (U.compareAndSwapObject(a, j, t, null)) {
                        U.putOrderedInt(this, QTOP, s - 1);
                        return t;
                    }
                    break;
                }
                else if ((r = r.completer) == null) // 沿着父子关系,直到根
                    break;
            }
        }
    }
    return null;
}

mode < 0 —— 必须加锁路径:   外部线程/高竞争场景
mode >= 0 —— 无锁快路径:   当前线程拥有队列（worker 自身）
```