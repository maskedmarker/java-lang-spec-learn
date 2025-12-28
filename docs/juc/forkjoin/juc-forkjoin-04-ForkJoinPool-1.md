# juc-forkjoin-ForkJoinPool


ForkJoinPool的任务要么待执行在WorkQueue中保存着,要么正在被执行而从WorkQueue移出处于方法栈中保存着.💯💯💯
提交任务不用submit而用fork;join并非真正的直接挂起等待,而是去帮助被join任务了.



## 关键字段


workQueues
```text
ForkJoinPool.workQueues数组在初始化后不会动态扩容,workQueues数组元素按需新增,新增后会一直保留不被清除,即对应的线程也不会销毁,而是挂起避免空转; 💯💯💯
WorkQueue.array会扩容但不会缩容,array数组元素在任务结束后会主动清除.

workQueues数组的长度就是用户设定或者cpu核心线程数.由于fork-join框架是为计算密集型任务设计的,所以线程池线程数不能大于cpu核心线程数.当不然用户设定的线程数也不要大于cpu核心线程数.
```


runState
```text
private static final int  RSLOCK     = 1;
private static final int  RSIGNAL    = 1 << 1;
private static final int  STARTED    = 1 << 2;     // 已经初始化
private static final int  STOP       = 1 << 29;    // Terminating 注意ing
private static final int  TERMINATED = 1 << 30;    // Terminated
private static final int  SHUTDOWN   = 1 << 31;    // Shutting down

只有lockRunState/unlockRunState能修改runState,其他方法只能读runState
```


```text
volatile long ctl;                   // main pool control


// Active counts 
private static final int  AC_SHIFT   = 48;
private static final long AC_UNIT    = 0x0001L << AC_SHIFT;
private static final long AC_MASK    = 0xffffL << AC_SHIFT;         //  用16-bit表示活跃线程数,也就是线程池最大支持大约2^15个线程

// Total counts
private static final int  TC_SHIFT   = 32;
private static final long TC_UNIT    = 0x0001L << TC_SHIFT;
private static final long TC_MASK    = 0xffffL << TC_SHIFT;          // 用16-bit表示总线程数
private static final long ADD_WORKER = 0x0001L << (TC_SHIFT + 15); // sign


---------------------------------------------------------------------------------------------------------------------------

64-bits ctl被拆成四段： ctl = (AC << 48) | (TC << 32) | (ST << 31) | SS
+---------------------------+---------------+-----+--------------------+
|   AC (activeCount:16)     | TC (total:16) | ST  |    SS (stack:31)   |
+---------------------------+---------------+-----+--------------------+
 63                       48 47           32 31    30                 0

AC = Active Count       (bits 63..48)
TC = Total Count        (bits 47..32)
ST = version/stop bit   (bit 31)
SS = stack pointer      (bits 30..0)



💯💯💯💯💯
活跃指的是工作线程正在找可以偷的任务/已经偷到任务将要执行任务/正在执行任务;非活跃指的是工作线程找不到可以偷的任务,准备或已经挂起等待. (工作线程被设计为隐藏在工作队列背后,对于活跃的工作线程,也可以称呼其工作队列为活跃的工作队列)
ForkJoinPool线程池中的工作线程是创建后是不会销毁的,无任务执行时,挂起等待进入不活跃状态.
由于工作线程创建后不会销毁,所以active-count/total-count为负数时,表示还需要创建线程.(线程池初始值就是负数)
当线程数达到最大并行度后,total-count值就不会再改变,同时即使工作线程挂起进入不活跃后,active-count最小也就为0.


⭐AC：Active Count(16 bits,带符号)
表示当前活跃的工作线程数.(负数表示当前线程池中线程数还没有达到最大并行度,如果当前活跃线程数不够,还可以再创建线程.)
提取方式：int ac = (int)(ctl >> 48);


⭐TC：Total Count(16 bits,带符号)
表示线程池中存在的线程数量  (负数表示当前线程池中线程数还没有达到最大并行度,如果还需要工作线程,还可以再创建线程.)
提取方式：int tc = (int)(ctl >> 32);

⭐ ST：Stop标志(1 bit)
位于第 31 位(仅 1 bit)
具体含义随版本略变,但一般用于：“版本号”/“冲突规避”/“终止标志”
大部分资料称此位为 ctl’s epoch bit.


⭐ SS：Stack pointer(31 bits)
ctl的低31位保存不活跃工作队列的WorkQueue.scanState值.当有多个不活跃工作队列时,通过stackPred字段形成一个stack栈数据结构,ctl的低31位保存的就是栈顶值. (不活跃线程的scanState值都不同)
提取方式：int sp = (int)ctl;
如果 sp =0,表示没有不活跃线程,即没有空闲线程.

```


## 构造函数

1. 设置线程从本地WorkQueue获取任务的模式(LIFO/FIFO)
2. 设置线程池初始态为还需要创建parallelism个线程


```text
private ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, int mode, String workerNamePrefix) {
    this.workerNamePrefix = workerNamePrefix;
    this.factory = factory;
    this.ueh = handler;
    this.config = (parallelism & SMASK) | mode; // config的16th-bit是用来设置从本地WorkQueue中获取任务的模式(LIFO/FIFO)  低16-bits用来表示并行度,最大并行度为2^16-1
    
    long np = (long)(-parallelism);
    this.ctl = ((np << AC_SHIFT) & AC_MASK) | ((np << TC_SHIFT) & TC_MASK);  
    // [((-parallelism) << AC_SHIFT) & AC_MASK]设置ctl的初始态Active-Count为-parallelism,表示还需要创建parallelism个线程💯
    // [((np << TC_SHIFT) & TC_MASK)]设置ctl的初始态Total-Count为-parallelism,表示还需要创建parallelism个线程💯
}
```


## 向ForkJoinPool线程池提交任务

```text
// 提交任务并等待任务执行完毕(同步执行)
public <T> T invoke(ForkJoinTask<T> task) {
    if (task == null)
        throw new NullPointerException();
    
    // 向线程池提交任务
    externalPush(task);
    
    return task.join(); // 等待执行完成
}

// 提交任务但不等待任务执行完毕(异步执行)
public void execute(ForkJoinTask<?> task) {
    if (task == null)
        throw new NullPointerException();
        
    externalPush(task); // 提交任务后,不用等待任务执行完成
}


------------------------------ 其他变种 ------------------------------------------
入参Runnable/Callable需要封装到ForkJoinTask

// 异步执行
public void execute(Runnable task) {
    if (task == null)
        throw new NullPointerException();
    ForkJoinTask<?> job;
    if (task instanceof ForkJoinTask<?>) // avoid re-wrap
        job = (ForkJoinTask<?>) task;
    else
        job = new ForkJoinTask.RunnableExecuteAction(task);
    externalPush(job);
}

// 异步执行
public <T> ForkJoinTask<T> submit(Callable<T> task) {
    ForkJoinTask<T> job = new ForkJoinTask.AdaptedCallable<T>(task);
    externalPush(job);
    return job;
}
```

## externalPush

```text
final void externalPush(ForkJoinTask<?> task) {
    WorkQueue[] ws; WorkQueue q; int m;
    int r = ThreadLocalRandom.getProbe();
    int rs = runState;
    
    // 提交任务的fast-path
    if ((ws = workQueues) != null && (m = (ws.length - 1)) >= 0 &&      // 如果ForkJoinPool已经初始化
        (q = ws[m & r & SQMASK]) != null && r != 0 && rs > 0 &&         // 且随机到了一个共享队列(偶数索引) 且ForkJoinPool没有处于关闭流程中
        
        U.compareAndSwapInt(q, QLOCK, 0, 1)) {                          // 对工作队列加锁成功
        
        ForkJoinTask<?>[] a; int am, n, s;
        if ((a = q.array) != null &&
            (am = a.length - 1) > (n = (s = q.top) - q.base)) {         // 队列中任务数大于一(降低与steal操作的并发)
            
            int j = ((am & s) << ASHIFT) + ABASE;                       // 计算数组的top索引的地址偏移量
            U.putOrderedObject(a, j, task);                             // 使用native方法,在数组的top处添加一个元素
            
            U.putOrderedInt(q, QTOP, s + 1);                            // 队列的top++
            
            U.putIntVolatile(q, QLOCK, 0);                              // 对工作队列解锁
            
            if (n <= 1)                                                 // 之前队列中任务基本处理完了(小于等于1时),工作队列的工作线程线程很可能已经因为没有任务而挂起等待了,现在又有新任务了,需要尝试唤醒它
                signalWork(ws, q);
            return;
        }
        
        U.compareAndSwapInt(q, QLOCK, 1, 0);                            // 对工作队列解锁
    }
    
    externalSubmit(task);    // 其他场景使用完整版的提交方法
}
```

## externalSubmit

Full version of externalPush, handling uncommon cases, as well as performing secondary initialization upon the first submission of the first task to the pool. 
It also detects first submission by an external thread and [creates a new shared queue] if the one at index if empty or contended.

externalSubmit触发ForkJoinPool的初始化:
    实例化workQueues数组,但是数组中并没有元素.
    随机选定一个偶数索引值,创建一个共享任务队列,注册到数组的该偶数索引处
    将提交的任务压入到该偶数索引处的共享任务队列
    通知ForkJoinPool创建更多的工作线程并为工作线程创建并配置工作任务队列(后续这些工作线程会从共享任务队列steal任务)


备注:
1. 整体上来看,工作任务的子任务会放入到本地工作任务队列,也就是奇数索引;而共享任务队列使用的是偶数索引. 结论就是workQueues数组中,偶数索引处是共享/提交任务队列(shared-queue/submmission-queue),而奇数索引处是工作任务队列(work-queue).💯💯💯



💯外部提交的任务都是top-level task,由top-level task衍生出的子任务则不是.

```text
private void externalSubmit(ForkJoinTask<?> task) {
    int r;                                    // r是个随机数,将task分配到ws[r]
    if ((r = ThreadLocalRandom.getProbe()) == 0) {
        ThreadLocalRandom.localInit();
        r = ThreadLocalRandom.getProbe();
    }
    
    
    for (;;) {
        WorkQueue[] ws; WorkQueue q; int rs, m, k;
        boolean move = false;
        
        // 如果提交任务时,发现runState状态为关闭
        if ((rs = runState) < 0) {
            tryTerminate(false, false);     // help terminate
            throw new RejectedExecutionException();
        }
        
        // ForkJoinPool未初始化,触发ForkJoinPool初始化操作(runState只有在未初始化时才会等于0,之后再也不会是0)
        else if ((rs & STARTED) == 0 ||
                 ((ws = workQueues) == null || (m = ws.length - 1) < 0)) {
            int ns = 0;
            rs = lockRunState();
            try {
                if ((rs & STARTED) == 0) {                                                 // checkCondition-加锁-recheckCondition-executeCriticalSection这是使用锁的标准步骤    
                    U.compareAndSwapObject(this, STEALCOUNTER, null, new AtomicLong());    // 初始化stealCounter
                    
                    // 初始化ForkJoinPool.workQueues,数组长度必须是2^n(后面大量依赖于workQueues.length-1作&运算来实现取模的效果)
                    int p = config & SMASK;      // SMASK = 0xffff p最大为2^16-1
                    int n = (p > 1) ? p - 1 : 1;  // n最大为2^16-2
                    n |= n >>> 1; n |= n >>> 2;  n |= n >>> 4;
                    n |= n >>> 8; n |= n >>> 16; n = (n + 1) << 1;  // workQueues数组的大小为大于等于并行度p的最小2的幂再翻一倍  (2的幂是为了方便计算;再翻一倍是因为非共享工作队列只使用奇数索引) 
                    workQueues = new WorkQueue[n];  // n的最大值为2^17
                    ns = STARTED;
                }
            } finally {
                unlockRunState(rs, (rs & ~RSLOCK) | ns);                                    // 释放ForkJoinPool的锁,并唤醒等待的线程
            }
        }
        
        // ForkJoinPool已初始化,为入参task在workQueues中随机选定一个偶数索引值的workQueue,如果此时对应位置的workQueue已经存在则将task压入top处
        else if ((q = ws[k = r & m & SQMASK]) != null) {   // SQMASK=(1111110)2  (k = r & m & SQMASK)保证k是2^7=128内的偶数,即最多64个💯💯💯 并没有用完(2^17-1)/2个理论空间
            // workQueue是无锁队列,使用cas+重试来完成操作
            if (q.qlock == 0 && U.compareAndSwapInt(q, QLOCK, 0, 1)) {
                ForkJoinTask<?>[] a = q.array;
                int s = q.top;
                boolean submitted = false; // initial submission or resizing
                try {                      // locked version of push
                    if ((a != null && a.length > s + 1 - q.base) ||   
                        (a = q.growArray()) != null) {                       // workQueue的数组容量不够的话,需要翻倍扩容
                        
                        int j = (((a.length - 1) & s) << ASHIFT) + ABASE;
                        U.putOrderedObject(a, j, task);
                        U.putOrderedInt(q, QTOP, s + 1);                     // 将task添加到top处,并top++
                        submitted = true;
                    }
                } finally {
                    U.compareAndSwapInt(q, QLOCK, 1, 0);
                }
                
                // 提交任务成功,唤醒task所属的workQueue
                if (submitted) {
                    signalWork(ws, q);
                    return;
                }
            }
            move = true;                   // 如果对workQueue加锁失败,通过循环重试. 本次的随机数已经用过了,下一循环使用新的随机数
        }
        
        
        // ForkJoinPool已初始化,为入参task在workQueues中随机选定一个workQueue,如果此时对应位置的workQueue不存在则创建一个新的workQueue (注意: 并没有直接将task提交到新的workQueue;由于r也未更新,则意味着下个循环中会在workQueues的相同位置压入task)
        else if (((rs = runState) & RSLOCK) == 0) {
            q = new WorkQueue(this, null);  // 这个workQueue没有绑定工作线程.那么它的任务是用来被其他线程steal,即创建了一个共享工作队列💯💯💯
            q.hint = r;                    // WorkQueue.hint的初始值来自于提交任务线程的一个随机数
            q.config = k | SHARED_QUEUE;   // WorkQueue.config的31th-bit初始值标识SHARED_QUEUE
            q.scanState = INACTIVE;        // WorkQueue.scanState的初始值为INACTIVE
            
            rs = lockRunState();           
            
            // 将新workQueue注册到ForkJoinPool.workQueues[k]中, k是128内的偶数,即共享队列都是偶数索引值
            if (rs > 0 &&  (ws = workQueues) != null &&
                k < ws.length && ws[k] == null)
                ws[k] = q;
            // 如果不满足if条件,意味着ForkJoinPool发生terminated    
            
            unlockRunState(rs, rs & ~RSLOCK);
        }
        else
            move = true;                   // 下一循环使用新的随机数
        
        // 本次的随机数已经用过了,下一循环使用新的随机数
        if (move)
            r = ThreadLocalRandom.advanceProbe(r);
    }
}
```

## signalWork

Tries to create or activate a worker if too few are active


```text
final void signalWork(WorkQueue[] ws, WorkQueue q) {
    long c; int sp, i; WorkQueue v; Thread p;
    
    while ((c = ctl) < 0L) {                       // 当ctl为负数时表示还缺线程(active-account初始值为-parallelism)
        
        // 如果没有空闲的工作线程且还未达到cpu最大核心线程数则新增工作线程
        if ((sp = (int)c) == 0) {                  // ctl-low32保存WorkQueue.scanState,如果ctl-low32为0,表示没有空闲的工作线程
            if ((c & ADD_WORKER) != 0L)            // Total-Count为负数表示线程池现有线程数还未达到cpu最大核心线程数,还可以新增工作线程
                tryAddWorker(c);
            break;
        }
        // else: 还有空闲线程/已经达到核心线程数

        
        if (ws == null)                            // 避开unstarted/terminated
            break;
        if (ws.length <= (i = sp & SMASK))         // 避开terminated
            break;
        if ((v = ws[i]) == null)                   // 避开terminating
            break;
        
        
        // 唤醒inactive stack的栈顶的空闲工作线程
        int vs = (sp + SS_SEQ) & ~INACTIVE;        // next scanState
        int d = sp - v.scanState;                  // screen CAS
        long nc = (UC_MASK & (c + AC_UNIT)) | (SP_MASK & v.stackPred);
        if (d == 0 && U.compareAndSwapLong(this, CTL, c, nc)) {
            v.scanState = vs;                      
            if ((p = v.parker) != null)
                U.unpark(p);                      //先设置scanState为active再unpark
            break;
        }
        
        if (q != null && q.base == q.top)          // 通过入参q的工作队列是否为空来大概估计整体是否还有任务,如果没有了即使线程工作线程还未达到cpu核心线程数,也不要再新增工作线程了
            break;
    }
}
```

## tryAddWorker

ctl的29th-bit是STOP位

如果ForkJoinPool正在terminating,则放弃新增工作线程.
先active-count/total-count加一再创建一个工作线程.

```text
private void tryAddWorker(long c) {
    boolean add = false;
    do {
        long nc = ((AC_MASK & (c + AC_UNIT)) |        // active-count加一
                   (TC_MASK & (c + TC_UNIT)));        // total-count加一
        
        // 在每次循环开始时,都要读取最新的ctl值到变量c
        if (ctl == c) {
            int rs, stop;                 
            // 先判断ForkJoinPool是否正在terminating;只有在ForkJoinPool正常工作态(非terminating)下才去cas更新active-count/total-count
            if ((stop = (rs = lockRunState()) & STOP) == 0)       
                add = U.compareAndSwapLong(this, CTL, c, nc); // 并发tryAddWorker会出现cas失败,通过重试避免
            
            
            unlockRunState(rs, rs & ~RSLOCK);
            
            // 如果ForkJoinPool正在terminating,则不再新增工作线程
            if (stop != 0)
                break;
            
            // 只有在ForkJoinPool正常工作态(非terminating)才添加工作线程
            if (add) {
                createWorker();
                break;
            }
        }
    } while (((c = ctl) & ADD_WORKER) != 0L && (int)c == 0);   // 当线程池还需要创建线程且无空闲工作线程(空闲工作线程可以steal-work)   注意这里c=ctl,c读取最新的ctl值
}

[((c = ctl) & ADD_WORKER) != 0L] 表示total-count为负数,表示还需要创建线程.
[(int)ctl == 0] 表示没有空闲的工作线程
```

## createWorker

```text
private boolean createWorker() {
    ForkJoinWorkerThreadFactory fac = factory;
    Throwable ex = null;
    ForkJoinWorkerThread wt = null;
    try {
        if (fac != null && (wt = fac.newThread(this)) != null) {  // 通过线程工厂创建工作线程(在工作线程的构造函数中,工作线程的构造函数会调用ForkJoinPoll.registerWorker,让ForkJoinPoll为工作线程分配一个WorkQueue)
            wt.start();         // 并启动工作线程
            return true;
        }
    } catch (Throwable rex) {
        ex = rex;
    }
    
    // 如果创建线程对象发生异常
    deregisterWorker(wt, ex);
    
    return false;
}
```

## registerWorker

Callback from ForkJoinWorkerThread constructor to establish and record its WorkQueue.

ForkJoinWorkerThreadFactory在创建ForkJoinWorkerThread时会使用到该callback
为工作线程分配的私有工作队列在workQueues的索引都是奇数💯(共享工作队列的索引奇数偶数都有可能)

```text
final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
    UncaughtExceptionHandler handler;
    wt.setDaemon(true);                           // configure thread
    if ((handler = ueh) != null)
        wt.setUncaughtExceptionHandler(handler);
    
    // 创建WorkQueue
    WorkQueue w = new WorkQueue(this, wt);
    int i = 0;                                    // assign a pool index
    int mode = config & MODE_MASK;
    int rs = lockRunState();
    try {
        WorkQueue[] ws; int n;                    // skip if no array
        if ((ws = workQueues) != null && (n = ws.length) > 0) {
            int s = indexSeed += SEED_INCREMENT;                               // ForkJoinPool.indexSeed用来计算新workQueue的数组索引     |  (indexSeed+=SEED_INCREMENT)实现自增
            int m = n - 1; // n的最大值为2^17,意味着i的最大值为2^17-1,即只会占用0th-16th的bits💯💯💯
            
            // 如果该workQueues[i]已经有值了,需要重新再找一个空索引地址
            i = ((s << 1) | 1) & m;               // i是正奇数  (初始工作线程没有执行工作任务,所以scanState初始值设置为正奇数,表示scanning)    |  (((s << 1) | 1) & m)中的(|1)保证了i是奇数💯
            if (ws[i] != null) {                  
                int probes = 0;
                
                // 为了保证i是奇数,step必须保证是偶数,这样while循环就能将workQueues的所有偶数索引值都尝试一遍(至于step取值约n/2仅仅是因为简便)
                int step = (n <= 4) ? 2 : ((n >>> 1) & EVENMASK) + 2;  // step by approx half n
                while (ws[i = (i + step) & m] != null) {
                    // 当workQueues的所有索引都尝试一遍后,还找不到空索引,此时需要对workQueues扩容
                    if (++probes >= n) {
                        workQueues = ws = Arrays.copyOf(ws, n <<= 1); // workQueues翻倍扩容,原来的元素索引值保持不变💯
                        m = n - 1;
                        probes = 0;                                   // 扩容后冲突探针重新计数;m=mask换为新值
                    }
                }
            }
            
            // 此时为新工作队列在workQueues数组中找到了一个未使用的奇数索引值
            w.hint = s;                           // 随机数供后面使用(共享队列的hint取自提交线程的probe,工作线程的工作队列hint取自indexSeed)
            w.config = i | mode;                  // 获取本地任务时,LIFO或FIFO
            w.scanState = i;                      // (此时i必然是奇数)scanState初始值是正奇数,表示scanning
            ws[i] = w;                            // 将创建的WorkQueue注册到ForkJoinPool.workQueues
            
            //(共享工作队列的scanState保持1<<31不变,inactive) 非共享工作队列的scanState初始值借用索引值(正奇数,都不同) (不同正奇数 即1st~16th的bit序列不同, 0th-bit都是1, 31th-bit都是0);
            // 因为scanState的0th-bit被设计成了scan状态位,所以设置scanning的操作(scanState|1) 和设置not-scanning的操作(scanState&(~1))中,1st~16th的bit序列保持不变而只修改0th-bit
            // 因为scanState的31th-bit被设计成了active状态位,所以设置inactive的操作(scanState|(1<<31)) 和设置active的操作(scanState&(~(1<<31)))中,0st~16th的bit序列保持不变而只修改31th-bit
            
            // 设置scanning/not-scanning inactive/active的操作都没有改动过1th~16th的bit序列; 
            // 💯💯💯空闲工作线程通过ctl-low32和stackPred就能串联成一个无锁stack,无锁stack依赖于cas,为了防止cas的ABA问题,要求stack中的inactive工作队列的scanState必须保持唯一性,其他场景不要求唯一性
            // tryRelease和signalWorker会导致scanState自增(1<<16).在scanState初始不同的情况下,
            //   当连续发生了2^16次自增后发生溢出才可能与一个active的scanState相同 (不可能发生这个场景,因为自增意味着steal-work都失败,没有任务执行,另一个工作队列怎么可能一直保持有任务执行)
            //   当连续发生了2^32次自增后发生溢出才可能与一个仅发生一次自增的inactive的scanState相同(不可能发生这个场景,原因与前面一样)
            // 结论就是inactive工作队列的scanState能保持唯一性.                                                               
           
        }
    } finally {
        unlockRunState(rs, rs & ~RSLOCK);
    }
    
    // 工作队列在workQueues的索引值,不会因为workQueues扩容而改变,索引工作线程name中的序号不会相同
    wt.setName(workerNamePrefix.concat(Integer.toString(i >>> 1)));
    
    return w;
}
```

## runWorker

工作线程执行的main-loop逻辑
工作线程创建时ForkJoinPool为其分配WorkQueue;WorkQueue.scanState初始值是奇数,表示scanning.
然后启动线程的start方法,start方法会执行runWorker的main-loop逻辑,开始scan->从其他工作队列中获取一个任务->执行该任务及其子任务->等待新任务->scan的循环.


```text
final void runWorker(WorkQueue w) {
    w.growArray();                   // w就是刚刚为工作线程分配的工作队列,此时内部数组还未实例化,所以需要先分配一个数组
    int seed = w.hint;               // initially holds randomization hint
    int r = (seed == 0) ? 1 : seed;  // avoid 0 for xorShift
    
    // 工作线程main-loop💯
    for (ForkJoinTask<?> t;;) {
        // 此时工作队列w是空的
        if ((t = scan(w, r)) != null)
            w.runTask(t);  // t被当作top-level任务,执行该任务及其子任务
        
        else if (!awaitWork(w, r))
            break;
        
        r ^= r << 13; r ^= r >>> 17; r ^= r << 5; // 每循环一次,就通过xorshift算法再次生成一个伪随机数
    }
}
```

## scan

Scans for and tries to steal a top-level task.
Scans start at a random location, randomly moving on apparent contention, otherwise continuing linearly until reaching two consecutive empty passes over all queues with the same checksum (summing each base index of each queue, that moves on each steal), 
at which point the worker tries to inactivate and then re-scans, attempting to re-activate (itself or some other worker) if finding a task; 
otherwise returning null to await work. Scans otherwise touch as little memory as possible, to reduce disruption on other scanning threads.


scanState;    // versioned, <0: inactive; odd:scanning

```text
private ForkJoinTask<?> scan(WorkQueue w, int r) {   // w为非共享工作队列,w的工作线程是当前线程
    WorkQueue[] ws; int m;
    if ((ws = workQueues) != null && (m = ws.length - 1) > 0 && w != null) {  // 此时工作队列w是空的
        int ss = w.scanState;                                                 // 初始值是正奇数
        
        
        // 无限循环的出口: 1.steal任务成功 2.
        for (int origin = r & m, k = origin, oldSum = 0, checkSum = 0;;) {
            WorkQueue q; ForkJoinTask<?>[] a; ForkJoinTask<?> t;
            int b, n; long c;
            
            // 随机选取工作队列ws[k]
            if ((q = ws[k]) != null) {
                if ((n = (b = q.base) - q.top) < 0 && (a = q.array) != null) {         // base<top即base<=top-1 工作队列不为空
                    long i = (((a.length - 1) & b) << ASHIFT) + ABASE;
                    if ((t = ((ForkJoinTask<?>)U.getObjectVolatile(a, i))) != null &&  // 查看base处的任务
                        q.base == b) {                                                 // 降低并发steal/pop的可能性(优化手段)
                        if (ss >= 0) {                                                 // scanState>0代表普通工作队列且工作线程正在执行任务  scanState=0代表共享工作队列
                            if (U.compareAndSwapObject(a, i, t, null)) {               // 通过cas完成steal-working
                                q.base = b + 1;
                                
                                // top-base>1,被偷工作队列还有任务,让更多线程一起工作尽快执行完队列中的任务(如果base处steal-work失败,意味着被偷工作队列马上要没任务,不需要更多的线程)
                                if (n < -1)                                           
                                    signalWork(ws, q);
                                return t;
                            }
                            // else意味着没有偷到任务,也就是队列基本为空,不需要更多的线程帮忙执行任务
                        }
                        else if (oldSum == 0 &&                                      
                                 w.scanState < 0)                                        // scanState<0表示入参队列因为???需要挂起
                            tryRelease(c = ctl, ws[m & (int)c], AC_UNIT);                // 从ctl低32-bit获取最后idle的工作队列的scanState, 然后数组索引值
                    }
                    
                    
                    // 在工作队列ws[k]steal-work失败,再随机选一个工作队列重新scan (在rescan前重置checkSum/oldSum)
                    if (ss < 0)                   // refresh
                        ss = w.scanState;
                    r ^= r << 1; r ^= r >>> 3; r ^= r << 10;
                    origin = k = r & m;                                 
                    oldSum = checkSum = 0;
                    continue;                               
                }
                checkSum += b; // checkSum来自各个工作队列的base值
            }
            
            // 遍历2遍workQueues后,仍未steal到任务
            if ((k = (k + 1) & m) == origin) {    // ++k                    
                if ((ss >= 0 || (ss == (ss = w.scanState))) &&                  // 当要执行(ss == (ss = w.scanState)时,此时ss<0,重新读取scanState到遍历ss,对比新旧ss值
                    oldSum == (oldSum = checkSum)) {                   // 连续2遍各个工作队列的base值都没变,而且还没steal到任务,意味着没有新任务了.
                    
                    // already inactive
                    if (ss < 0 || w.qlock < 0)                         // ss < 0表示工作线程inactive;w.qlock < 0表示工作线程terminate???
                        break;
                    
                    // 更新当前工作队列状态scanState为inactive,并将工作队列状态保存到ctl-low32中
                    int ns = ss | INACTIVE;                               // 
                    long nc = ((SP_MASK & ns) |                           // 新ctl值中低32-bit保存最新inactive工作队列的scanState   (非共享工作队列的scanState时刻保持不同,且1st~30th的bit序列保持不变,可以将scanState作为工作队列的唯一标识)💯💯💯
                               (UC_MASK & ((c = ctl) - AC_UNIT)));        // 高32-bit中的active-count减一(因为不是销毁工作线程,所以total-count不变)
                    
                    w.stackPred = (int)c;                                 // ctl-low32记录最近inactive的工作队列标识,stackPred保留ctl值中低32-bit的旧值,这样通过ctl-low32和stackPred就能串联成一个stack💯💯💯
                    U.putInt(w, QSCANSTATE, ns);
                    if (U.compareAndSwapLong(this, CTL, c, nc))
                        ss = ns;
                    else
                        w.scanState = ss;         // cas失败则恢复w.scanState到最后读取的旧值,然后重试
                }
                checkSum = 0;
            }
        }
    }
    return null;
}


ctl-low32和stackPred串联成一个stack正常工作的前提是任意时刻inactive的工作线程的scanState要保持互不相同.
工作线程scanState的初始值是不同的.
signalWork/tryRelease会导致scanState自增
```

## awaitWork

找不到要执行的任务,先有限自旋然后再park(timeout)挂起等待. (不支持中断)

```text
private boolean awaitWork(WorkQueue w, int r) {
    if (w == null || w.qlock < 0)                 // w is terminating
        return false;
    
    // 进入awaitWork前,当前线程已经将w.scanState设置为inactive
    
    
    // 在循环中等待任务到来,先自旋后挂起
    for (int pred = w.stackPred, spins = SPINS, ss;;) {
        if ((ss = w.scanState) >= 0)   // 循环等待的出口是被tryRelease唤醒
            break;
        
        // 工作线程先有限自旋然后再park(timeout)挂起等待,等待被tryRelease唤醒去执行新增的任务   (当前SPINS为0,可以不考虑这段)  
        else if (spins > 0) {
            r ^= r << 6; r ^= r >>> 21; r ^= r << 7;
            if (r >= 0 && --spins == 0) {         // randomize spins
                WorkQueue v; WorkQueue[] ws; int s, j; AtomicLong sc;
                if (pred != 0 && (ws = workQueues) != null &&
                    (j = pred & SMASK) < ws.length &&
                    (v = ws[j]) != null &&        // see if pred parking
                    (v.parker == null || v.scanState >= 0))
                    spins = SPINS;                            // 如果idle-stack中当前节点的前节点工作线程已经active了,就再自旋一会,估计马上当前节点的线程就要被tryRelease了
            }
        }
        else if (w.qlock < 0)                     // recheck after spins
            return false;
        
        // 进入挂起等待逻辑
        else if (!Thread.interrupted()) {                                        // 清除中断
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
            if (w.scanState < 0 && ctl == c)      // 在park前,再次确认scanState是inactive,防止此时被tryRelease
                U.park(false, parkTime); 
            // 结束park了,清除park前的设置
            U.putOrderedObject(w, QPARKER, null);
            U.putObject(wt, PARKBLOCKER, null);
            
            if (w.scanState >= 0)              // park结束后再确认下是否被tryRelease
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

## tryRelease

如果入参的工作队列位于inactive stack的栈顶,则唤醒它.

```text
private boolean tryRelease(long c, WorkQueue v, long inc) {
    int sp = (int)c, vs = (sp + SS_SEQ) & ~INACTIVE; Thread p;
    
    if (v != null && v.scanState == sp) {          // v is at top of stack
        long nc = (UC_MASK & (c + inc)) | (SP_MASK & v.stackPred);
        if (U.compareAndSwapLong(this, CTL, c, nc)) {  // 将栈顶元素指向的下一个元素设置为新栈顶
            v.scanState = vs;
            if ((p = v.parker) != null)
                U.unpark(p);                     // 先设置scanState为active再unpark
            return true;
        }
    }
    return false;
}
```

## awaitJoin

Helps and/or blocks until the given task is done or timeout.

awaitJoin方法名中有await单词,但是该方法并不是真让当前线程挂起等待,这样cpu的并行能力就得不到充分利用.
其次awaitJoin调用方是因为被调用方任务没有完成导致调用方任务无法进一步执行,所以核心点并不是调用方线程是否要挂起,只要调用方线程在被调用任务完成前不进一步执行调用方任务就行.

fork-join结构化范式中,当前线程刚fork/submit到自己工作队列中的新任务task被其他线程steal,现在当前工作线程只能等待task完成.
当前工作线程的"等待"
    可以是什么都不做:挂起
    也可以是帮助task尽快完成

当工作线程 w 在 join 一个 ForkJoinTask 时,在真正阻塞(park/wait)之前,尽一切可能主动推进该任务的完成;只有在“无法再推进”时,才允许线程进入等待态,并通过补偿机制维持池的并行度.

awaitJoin 是 ForkJoinPool 中 join 语义的“安全阀”：它在阻塞前通过 completion 推进与偷取协助最大化前进机会，并通过补偿机制保证即使发生阻塞也不会导致池内并行度塌陷或死锁

```text
final int awaitJoin(WorkQueue w, ForkJoinTask<?> task, long deadline) {
    int s = 0;
    if (task != null && w != null) {
        ForkJoinTask<?> prevJoin = w.currentJoin;
        U.putOrderedObject(w, QCURRENTJOIN, task);  // 表示当前worker正在等待的任务,currentSteal的用途请看helpStealer
        
        CountedCompleter<?> cc = (task instanceof CountedCompleter) ? (CountedCompleter<?>)task : null;  // CountedCompleter使用completion推进协议;普通ForkJoinTask使用steal/helpJoin协议
        
        // 任务结束或者等待时间超时,才能跳出循环
        for (;;) {
            if ((s = task.status) < 0)  // 快速完成检查
                break; // 任务结束
            
            if (cc != null)
                // CountedCompleter的join不应该阻塞,而应靠推进完成💯💯💯
                helpComplete(w, cc, 0);                             // helpComplete不会发生挂起,CountedCompleter的join是没有挂起等待,就是不断的执行.
            else if (w.base == w.top || w.tryRemoveAndExec(task))   // 如果task是普通ForkJoinTask
                // 如果当前工作队列为空,task不在本队列; 或者执行task失败
                helpStealer(w, task);                               // 找到正在偷该任务的worker,帮助其执行后续任务,防止join形成链式阻塞
            
            
            
            if ((s = task.status) < 0)  // 在后面挂起前再检查一下,避免不必要的阻塞
                break; // 任务结束
            long ms, ns;
            if (deadline == 0L)
                ms = 0L;
            else if ((ns = deadline - System.nanoTime()) <= 0L)
                break;  // 等待时间超时
            else if ((ms = TimeUnit.NANOSECONDS.toMillis(ns)) <= 0L)
                ms = 1L;
            
            if (tryCompensate(w)) {   // 在当前worker即将阻塞前,判断是否需要创建/唤醒一个补偿线程,以维持ForkJoinPool的并行度不下降
                task.internalWait(ms);
                U.getAndAddLong(this, CTL, AC_UNIT);
            }
        }
        U.putOrderedObject(w, QCURRENTJOIN, prevJoin);
    }
    return s;
}
```

调用awaitJoin的方法为如下:
```text
private int doJoin() {
    int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
    if((t = Thread.currentThread()) instanceof ForkJoinWorkerThread){  // 如果join方线程是内部线程
        if((w = (wt = (ForkJoinWorkerThread)t).workQueue).tryUnpush(this) && (s = doExec()) < 0){   // 将被join方的this_ForkJoinTask从其所属的工作队列中移除,且发现任务已经是终态,则直接返回
            return s;
        } else {
            return wt.pool.awaitJoin(w, this, 0L)   // 如果将被join方的this_ForkJoinTask从其所属的工作队列中移除失败,或者任务还未完成,就awaitJoin
        }
    }else {
            return externalAwaitDone();   // 如果join方线程是外部线程
    }	
}

private int doInvoke() {
    int s; Thread t; ForkJoinWorkerThread wt;       
    if(s = doExec()) < 0) {  // 如果任务已经是终态,直接返回
        return s;    
    } else {
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) {
            return (wt = (ForkJoinWorkerThread)t).pool.awaitJoin(wt.workQueue, this, 0L);
        } else {
            return externalAwaitDone();
        }
    }
}

public final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {      
    int s;
    long nanos = unit.toNanos(timeout);
    if (Thread.interrupted())
        throw new InterruptedException();
    if ((s = status) >= 0 && nanos > 0L) {
        long d = System.nanoTime() + nanos;
        long deadline = (d == 0L) ? 1L : d; // avoid 0
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
            s = wt.pool.awaitJoin(wt.workQueue, this, deadline);
        }
    //....    
}  
```

## helpComplete

helpComplete只会被awaitJoin调用.(请不要随意扩大用例的context)

工作线程w主动参与推进task(一个 CountedCompleter)所属计算子图的完成.

```text
final int helpComplete(WorkQueue w, CountedCompleter<?> task, int maxTasks) {
    WorkQueue[] ws; int s = 0, m;
    
    if ((ws = workQueues) != null && (m = ws.length - 1) >= 0 && task != null && w != null) {
        int mode = w.config;                 // for popCC
        int r = w.hint ^ w.top;              // arbitrary seed for origin
        int origin = r & m;                  // first queue to scan
        int h = 1;                           // 1:ran, >1:contended, <0:hash
        
        // 从随机位置开始扫描所有工作队列
        for (int k = origin, oldSum = 0, checkSum = 0;;) {
            CountedCompleter<?> p; WorkQueue q;
            if ((s = task.status) < 0)
                break; // 主要的循环退出口:task任务完成
            
            if (h == 1 && (p = w.popCC(task, mode)) != null) {   // 先在当前工作线程的工作队列中查找并执行task及其子任务(top处)
                p.doExec(); 
                if (maxTasks != 0 && --maxTasks == 0)                            // maxTasks != 0 && --maxTasks == 0 这样当maxTasks初始值为0时,就不会触发--maxTasks,从而当入参maxTasks为0时,不限制执行了多少个task子任务
                    break;
                origin = k;                  // reset
                oldSum = checkSum = 0;
            }
            else {                           // 当前工作线程的工作队列中找不到task及其子任务,去其他工作队列中寻找(主要针对非task的工作线程,即stealer线程)
                if ((q = ws[k]) == null)
                    h = 0;
                else if ((h = q.pollAndExecCC(task)) < 0)    // 在ws[k]工作队列中查找并执行task及其子任务(base处)
                    checkSum += h;
                
                // 可以继续循环重新尝试执行task及其子任务
                if (h > 0) {                                         // h值的含义来源于pollAndExecCC返回值的定义, 1-执行任务成功 2-因为并发没有执行成功可以重试 negative-执行失败且没必要重试
                    if (h == 1 && maxTasks != 0 && --maxTasks == 0)
                        break;
                    r ^= r << 13; r ^= r >>> 17; r ^= r << 5; // xorshift
                    origin = k = r & m;      // 不管是h=1/2,都再找一个其他的工作队列
                    oldSum = checkSum = 0;
                }
                else if ((k = (k + 1) & m) == origin) {
                    if (oldSum == (oldSum = checkSum))
                        break;   // 如果两次完整扫描,队列状态未变化,没有任务可帮,退出 helpComplete
                    checkSum = 0;
                }
            }
        }
    }
    return s;
}
```

## helpStealer

它解决的是Fork/Join中一个经典问题：我在等的任务,被别人偷走了,而那个人可能又在等别人.
当工作线程 w 在 join(task) 时,如果发现 task 已被其他 worker 偷走并正在执行,则沿着“偷取链”定位该 worker(及其后续 join 链),并主动帮其执行队列中的任务,从而推进 task 的完成,避免 join 阻塞.

```text
private void helpStealer(WorkQueue w, ForkJoinTask<?> task) {
    WorkQueue[] ws = workQueues;
    int oldSum = 0, checkSum, m;
    
    if (ws != null && (m = ws.length - 1) >= 0 && w != null && task != null) {
        
        // 最外层do–while:全局稳定性检测
        do {                                       // restart point
            checkSum = 0;                          // for stability check
            ForkJoinTask<?> subtask;
            WorkQueue j = w, v;                    // v is subtask stealer
            
            
            // descent循环:沿“偷取链”向下追踪. 只要subtask未完成,就尝试找出: 谁在执行它,它是否又join了别的任务
            descent: for (subtask = task; subtask.status >= 0; ) {
                for (int h = j.hint | 1, k = 0, i; ; k += 2) {                            // 只扫描奇数索引的非共享工作队列
                    if (k > m)                     // 扫描了一整圈,没有任何worker在偷subtask
                        break descent;
                    
                    if ((v = ws[i = (h + k) & m]) != null) {
                        if (v.currentSteal == subtask) {  // 💯💯💯currentSteal的用途在这里
                            j.hint = i;
                            break;
                        }
                        checkSum += v.base;
                    }
                }
                // 此时已经识别到stealer
                
                // 帮助stealer执行队列里的任务,或继续向下“追join”
                for (;;) {                         // help v or descend
                    ForkJoinTask<?>[] a; int b;
                    checkSum += (b = v.base);      // 
                    
                    ForkJoinTask<?> next = v.currentJoin;
                    if (subtask.status < 0 || j.currentJoin != subtask || v.currentSteal != subtask) // stale
                        break descent;
                    if (b - v.top >= 0 || (a = v.array) == null) {       // 队列为空
                        if ((subtask = next) == null)                    
                            break descent;
                        
                        // stealer自己也在join别的任务,去帮助stealer正在join的任务 💯💯💯
                        j = v;
                        break;
                    }
                    
                    // 尝试真正“帮忙”,从base处偷任务
                    int i = (((a.length - 1) & b) << ASHIFT) + ABASE;
                    ForkJoinTask<?> t = ((ForkJoinTask<?>) U.getObjectVolatile(a, i));
                    if (v.base == b) {
                        if (t == null)             // stale
                            break descent;
                        
                        // 执行一个stealer的任务,然后把当前工作线程的工作队列中的任务都执行完(当前正在等待join完成的任务不在工作队列,在方法栈中呢)
                        if (U.compareAndSwapObject(a, i, t, null)) {
                            v.base = b + 1;
                            ForkJoinTask<?> ps = w.currentSteal;
                            int top = w.top;
                            do {
                                U.putOrderedObject(w, QCURRENTSTEAL, t);
                                t.doExec();                                
                            } while (task.status >= 0 &&
                                     w.top != top &&
                                     (t = w.pop()) != null);               // t初始是从stealer的base偷来的,之后是当前工作线程的工作队列的top处的任务
                            U.putOrderedObject(w, QCURRENTSTEAL, ps);
                            
                            if (w.base != w.top)
                                return;            // 前工作线程的工作队列又来任务了,不帮了
                        }
                    }
                }
            }
        } while (task.status >= 0 && oldSum != (oldSum = checkSum));
    }
}
```


## 线程池管理

### shutdown

Possibly initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
no additional effect if already shut down. 
Tasks that are in the process of being submitted concurrently during the course of this method may or may not be rejected.

```text
public void shutdown() {
    tryTerminate(false, true); // now==false; enable==true
}
```

### tryTerminate

Possibly initiates and/or completes termination.

Params:
    now – if true, unconditionally terminate, else only if no work and no active workers   (true表示立即终止，false表示尝试终止)
    enable – if true, enable shutdown when next possible                                   (可以尝试推进到SHUTDOWN阶段)
Returns:
    true if now terminating or terminated

// tryTerminate有3 phases: SHUTDOWN, STOP, then TERMINATE


tryTerminate不是“发起关闭”,而是: 在 shutdown / shutdownNow / worker 退出 / steal 失败 等多个路径中,反复被调用,尝试推进池的终止状态机
👉 它是一个 “推进式终止器（termination progressor）”

```text
private boolean tryTerminate(boolean now, boolean enable) {
    int rs;
    // 作为fork-join框架的默认线程池,ForkJoinPool.common不能被关闭💯
    if (this == common)                       
        return false;
    
    if ((rs = runState) >= 0) {
        if (!enable)
            return false;   // 如果enable为false,发现当前ForkJoinPool还未进入SHUTDOWN阶段,直接放弃.
        
        rs = lockRunState();                  
        unlockRunState(rs, (rs & ~RSLOCK) | SHUTDOWN); // 设置runState表明进入SHUTDOWN阶段
    }

    if ((rs & STOP) == 0) {                                            // STOP=(1<<29)
        if (!now) {                           // check quiescence
            
            // 进入SHUTDOWN阶段:不再接受外部任务,但允许已提交任务完成
            for (long oldSum = 0L;;) {        // repeat until stable
                WorkQueue[] ws; WorkQueue w; int m, b; long c;
                long checkSum = ctl;
                if ((int)(checkSum >> AC_SHIFT) + (config & SMASK) > 0)
                    return false;             // still active workers
                if ((ws = workQueues) == null || (m = ws.length - 1) <= 0)
                    break;                    // 特例:因为未提交过任务而未初始化
                
                for (int i = 0; i <= m; ++i) {
                    if ((w = ws[i]) != null) {
                        if ((b = w.base) != w.top || w.scanState >= 0 ||
                            w.currentSteal != null) {
                            tryRelease(c = ctl, ws[m & (int)c], AC_UNIT);  // 发现工作队列还有任务,唤醒因无任务而挂起等待的工作线程,加快任务执行速度.
                            return false;                                  
                        }
                        checkSum += b;
                        if ((i & 1) == 0)     // 共享队列是偶数的
                            w.qlock = -1;     // 共享队列的qlock设置为-1,外部就无法再提交任务了
                    }
                }
                if (oldSum == (oldSum = checkSum))  // checkSum由ctl/base叠加而来
                    break;                          // 此时无活跃工作线程,而且base也不再变化,意味着无待执行的工作任务
            }
        }
        
        // 当工作线程进入不活跃后,完成shutdown阶段,进入stop阶段
        if ((runState & STOP) == 0) {
            rs = lockRunState();              
            unlockRunState(rs, (rs & ~RSLOCK) | STOP);  // enter STOP phase
        }
    }

    // 进入STOP阶段:中断工作线程,让工作线程结束
    int pass = 0;                             // 3 passes to help terminate
    for (long oldSum = 0L;;) {                // or until done or stable
        WorkQueue[] ws; WorkQueue w; ForkJoinWorkerThread wt; int m;
        long checkSum = ctl;
        if ((short)(checkSum >>> TC_SHIFT) + (config & SMASK) <= 0 ||
            (ws = workQueues) == null || (m = ws.length - 1) <= 0) {
            if ((runState & TERMINATED) == 0) {
                rs = lockRunState();          // done
                unlockRunState(rs, (rs & ~RSLOCK) | TERMINATED);
                synchronized (this) { notifyAll(); } // for awaitTermination
            }
            break;
        }
        
        for (int i = 0; i <= m; ++i) {
            if ((w = ws[i]) != null) {
                checkSum += w.base;
                w.qlock = -1;                 // 标注工作队列结束
                if (pass > 0) {
                    w.cancelAll();                                   // 将工作队列中待执行的任务取消达(兜底)
                    if (pass > 1 && (wt = w.owner) != null) {
                        if (!wt.isInterrupted()) {
                            try {             // unblock join
                                wt.interrupt();                       // 中断工作线程
                            } catch (Throwable ignore) {
                            }
                        }
                        if (w.scanState < 0)
                            U.unpark(wt);     // 非活跃线程恢复变活跃后,会看到工作队列结束了,之后工作线程正常退出.
                    }
                }
            }
        }
        if (checkSum != oldSum) {             // unstable
            oldSum = checkSum;
            pass = 0;
        }
        else if (pass > 3 && pass > m)        // can't further help
            break;
        else if (++pass > 1) {                // try to dequeue
            long c; int j = 0, sp;            // bound attempts
            while (j++ <= m && (sp = (int)(c = ctl)) != 0)
                tryRelease(c, ws[sp & m], AC_UNIT); // 非活跃线程恢复变活跃后才能正常退出
        }
    }
    return true;
}
```


## toString

toString是用来更直白的自然语言来描述各个状态,可以帮助理解各个字段所要表达的含义.

```text
public String toString() {
    // Use a single pass through workQueues to collect counts
    long qt = 0L, qs = 0L; int rc = 0;
    AtomicLong sc = stealCounter;
    long st = (sc == null) ? 0L : sc.get();
    long c = ctl;
    WorkQueue[] ws; WorkQueue w;
    if ((ws = workQueues) != null) {
        for (int i = 0; i < ws.length; ++i) {
            if ((w = ws[i]) != null) {
                int size = w.queueSize();
                if ((i & 1) == 0)
                    qs += size;
                else {
                    qt += size;
                    st += w.nsteals;
                    if (w.isApparentlyUnblocked())  // 工作线程正在运行,而非挂起等待
                        ++rc;
                }
            }
        }
    }
    
    int pc = (config & SMASK);
    int tc = pc + (short)(c >>> TC_SHIFT);
    int ac = pc + (int)(c >> AC_SHIFT);
    if (ac < 0) // ignore transient negative
        ac = 0;
    int rs = runState;
    String level = ((rs & TERMINATED) != 0 ? "Terminated" :
                    (rs & STOP)       != 0 ? "Terminating" :
                    (rs & SHUTDOWN)   != 0 ? "Shutting down" :
                    "Running");
    return super.toString() +
        "[" + level +
        ", parallelism = " + pc +
        ", size = " + tc +
        ", active = " + ac +
        ", running = " + rc +
        ", steals = " + st +
        ", tasks = " + qt +
        ", submissions = " + qs +
        "]";
}

pc ->  parallel core thread
tc ->  count of total worker threads
ac ->  counts of active worker threads
rc ->  count of running worker threads (rc与ac的不同在哪儿?  工作先将ac减一,再到park自己还有一段时间)
st ->  count of stolen works
qs ->  size of submission queue 
qt ->  count of tasks in working queue


---------------------------------------------------------------------------------------------------------------------
将三元运算符串起来,等价于if-elseif-else的控制结构

String level = ((rs & TERMINATED) != 0 ? "Terminated" :
                (rs & STOP)       != 0 ? "Terminating" :
                (rs & SHUTDOWN)   != 0 ? "Shutting down" :
                "Running");
                
if((rs & TERMINATED) != 0) {
    level = "Terminated";           
} else if((rs & STOP) != 0) {
    level = "Terminating";
} else if((rs & SHUTDOWN) != 0) {
    level = "Shutting down";
} else {
    level = "Running";
}
```
