# juc-future-CompletableFuture

## 从一个demo开始分析


```text
public void test01(){
    try {
        Supplier action1 = () -> { return "Hello"; };
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(action1); // 创建头计算阶段,其action是action1
        
        Consumer<?> action2 = System.out::println;
        CompletableFuture<Void> cf2 = cf.thenAccept(action2);   // 创建第二个计算阶段,其action是action2
    } catch (Exception e) {
        e.printStackTrace();
    }
}

CompletionStage: A stage of a possibly asynchronous computation, that performs an action or computes a value when another CompletionStage completes. 
每个CompletionStage代表一个计算阶段,即拥有一个对应action.当action执行完后,即这个计算阶段完成(绕过action直接设置也算计算阶段完成). 
这个计算阶段什么时候开始执行? 答:这个CompletionStage的上游CompletionStage完成时.
只要触发了最靠前的CompletionStage的完成,即可发生级联cascading完成.

CompletableFuture.supplyAsync(action1)声明了一个计算阶段及其计算逻辑(action1),其返回值即其声明的计算阶段(cf).
由于计算阶段的计算逻辑是异步的,所以要将计算阶段的计算逻辑由线程池来异步完成.
同时计算阶段的计算逻辑完成后会触发其计算阶段的完成,所以异步任务(对应AsyncSupply类)中要持有计算阶段及其计算逻辑.当异步任务执行时,异步任务执行计算阶段的计算逻辑后,还要标识计算阶段的完成(即设置result),并触发后续的级联完成(调用postComplete).

cf.thenAccept(action2)也是声明了一个计算阶段及其计算逻辑(action2),其返回值即其声明的计算阶段(cf2).
由于thenAccept方法支持同步运行cf2的计算逻辑(action2),
    如果此时cf2的上游计算阶段cf已经完成,直接同步执行cf2的计算逻辑(action2),并标识cf2计算阶段的完成(但不触发后续的级联完成,后面单独解释),然后结束thenAccept方法,返回一个已经完成的计算阶段(cf2).
    如果此时cf2的上游计算阶段cf还未完成,将.


CompletableFuture.supplyAsync(action1)向线程池提交了一个异步任务(被封装成asyncSupply,包含了本计算阶段cf及其要执行的action1),并返回一个completableFuture(该Future是relay角色的,该completableFuture也被封装到了asyncSupply). 
该异步任务AsyncSupply执行时会调用Supplier-action1,并将Supplier-action1的结果值设置为cf.result(表示头计算阶段完成),然后调用头计算阶段的postComplete完成后续的级联完成.

```


```text
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
    return asyncSupplyStage(asyncPool, supplier);        // 默认使用fork-join的common线程池
}

static <U> CompletableFuture<U> asyncSupplyStage(Executor e, Supplier<U> f) {
    if (f == null) throw new NullPointerException();
    
    CompletableFuture<U> d = new CompletableFuture<U>();  // 创建调用方通过supplyAsync方法声明的计算阶段
    e.execute(new AsyncSupply<U>(d, f));                  // 因为是头计算阶段,无法由上游触发完成计算任务,只能由线程池来完成计算阶段的计算任务.向线程池提交异步任务. (异步任务包含了计算阶段d和d的计算任务f)
    
    return d;   // 返回调用方声明的计算阶段
}



static final class AsyncSupply<T> extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {    // 为了支持普通线程池和fork-join线程池,任务需要extends ForkJoinTask (注意AsyncSupply并没有继承Completion,因为dep是头计算阶段)
    CompletableFuture<T> dep; Supplier<T> fn;
    
    AsyncSupply(CompletableFuture<T> dep, Supplier<T> fn) {                             // fn是计算阶段dep的计算任务
        this.dep = dep; this.fn = fn;
    }

    public final Void getRawResult() { return null; }
    public final void setRawResult(Void v) {}
    public final boolean exec() { run(); return true; }                                 // 在fork-join线程池,fork-join框架会保证任务一会被执行一次

    public void run() {
        CompletableFuture<T> d; Supplier<T> f;
        if ((d = dep) != null && (f = fn) != null) {                                    // 但是在普通线程池中,run方法就要自保,防止多次执行
            dep = null; fn = null;
            if (d.result == null) {
                try {
                    d.completeValue(f.get());                                           // 执行计算阶段d的计算任务f,然后将f的output值设置为计算阶段的result(设置result也标志着计算阶段d的完成)
                } catch (Throwable ex) {
                    d.completeThrowable(ex);                                            // 如果计算任务发生异常,需要将异常设置为计算阶段的result
                }
            }
            d.postComplete();                                                           // 根据CompletionStage接口的约定,当前计算阶段完成后需要执行(同步或异步是根据声明计算阶段时用的方法)下游的计算任务从而达到下游的完成
        }
    }
}
```

```text
public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
    return uniAcceptStage(null, action);
}

private CompletableFuture<Void> uniAcceptStage(Executor e, Consumer<? super T> f) {
    if (f == null) throw new NullPointerException();
    
    CompletableFuture<Void> d = new CompletableFuture<Void>();                           // 创建调用方通过thenAccept方法声明的计算阶段(当前计算阶段this是其上游)
    
    
    // 如果使用同步模式(e==null),则尝试现在是否能完成计算阶段d的计算任务f(只有上游现在已经完成才会尝试成功)
    // 如果是异步模式(e!=null),则由线程池完成计算阶段d的计算任务f
    if (e != null || !d.uniAccept(this, f, null)) {
        UniAccept<T> c = new UniAccept<T>(e, d, this, f);
        push(c);                                                                         // 如果尝试失败,就放入当前计算阶段this的stack中,等待当前计算阶段this完成后执行计算阶段d的计算任务f
        c.tryFire(SYNC);                                                                 // (tryFire保证只执行一次)防止发生(将c放入当前计算阶段this的stack与当前计算阶段this完成)的竟态问题,保证thenAccept在同步路径下不会被漏执行,即防止miss-fire
    }
    
    return d;   // 返回调用方声明的计算阶段
}


💯💯💯注意
uniAccept方法是fast-path,就是纯粹地设置CompletableFuture结果的方法,并没有在其内部调用postComplete()方法.
当fast-path成功时,不会用到上游stack的.只有当同步的fast-path失败(即需要退化成异步)和异步才会用到stack.
uniAccept是fast-path同步执行计算任务的方法,tryFire是异步执行计算任务的方法.由异步的tryFire承担postComplete(执行后续阶段的完成)以及清理上游stack(上游的stack是下游用来,且下游完成的晚,由下游负责清理更合理).


static final class UniAccept<T> extends UniCompletion<T,Void> {
    Consumer<? super T> fn;
    
    UniAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Consumer<? super T> fn) {     // fn是计算阶段dep的计算任务;src是dep的上游
        super(executor, dep, src); 
        this.fn = fn;
    }
    
    final CompletableFuture<Void> tryFire(int mode) {
        CompletableFuture<Void> d; CompletableFuture<T> a;
        
        // 如果UniAccept没有执行过(dep!=null),则尝试现在是否能完成计算阶段dep的计算任务fn(只有上游a现在已经完成才会尝试成功)
        // 异步mode不需要争抢认领Completion,同步和嵌入模式都会发生争抢,所以uniAccept的第三个入参需要非null
        if ((d = dep) == null || !d.uniAccept(a = src, fn, mode > 0 ? null : this))             // UniAccept只能执行一次,执行完会主动将dep设置为null.dep是否为null可以作为判断UniAccept是否执行过的依据
            return null;
        dep = null; src = null; fn = null;                                                      // 执行完会主动将dep设置为null
        
        
        // 如果d的上游a还未完成或nested模式则协助清理stack中无用的元素
        // 如果计算阶段d已经完成则执行下游计算任务.
        return d.postFire(a, mode);                                                            // postFire=协助(清理上游stack+执行上游postComplete)+本计算阶段postComplete
    }
}
```

```text
uniAccept方法解释:
uniAccept方法是实例方法,当前计算阶段即this对象.
uniAccept方法的入参a即当前计算阶段的上游;入参f即当前计算阶段的计算任务;入参c是上游a的completion-action即所谓的relay对象(后面详细讲解)
CompletableFuture.stack中存放的都是relay对象,relay对象用来衔接上游和下游,并控制并发调度.


// 尝试现在是否能完成当前计算阶段(this)的计算任务f
// 如果存在并发执行计算任务f的可能,在执行f前需要先认领c, 如果可以保证不会出现并发,可以将c设置为null💯
final <S> boolean uniAccept(CompletableFuture<S> a, Consumer<? super S> f, UniAccept<S> c) {        // 上游a;当前阶段的计算任务f; (包含了当前阶段this及其计算任务f)的上游a的completion-action c
    Object r; Throwable x;
    if (a == null || (r = a.result) == null || f == null)                      // 上游未完成则尝试失败(a == null, f == null都是防御代码)
        return false;
    
    
    tryComplete: if (result == null) {                                         // 如果上游完成且当前阶段还未完成,则执行当前阶段的计算任务
        if (r instanceof AltResult) {                                                   // decode AltResult
            if ((x = ((AltResult)r).ex) != null) {
                completeThrowable(x, r);                                                    // 如果上游以异常完成,当前计算阶段也以异常完成
                break tryComplete;
            }
            r = null;
        }
        
        try {
            // 防止当前计算阶段this的计算任务f被多次执行,需要先认领(防止多次被执行)
            // c.claim()返回true: 当前线程认领了当前计算阶段的计算任务f; c.claim()返回false: 其他线程认领了当前计算阶段的计算任务f. (如果当前计算阶段声明的是异步模式,则c.claim()总是返回false)   (c.claim()可以防止当前阶段的计算任务被多次执行,还可以控制同步异步模式)
            if (c != null && !c.claim())
                return false;
            
            @SuppressWarnings("unchecked") S s = (S) r;
            f.accept(s);
            completeNull();                                                              // 上游正常完成,当前计算阶段的计算任务正常执行完毕,设置当前计算阶段result为正常值null(因为当前计算阶段的计算任务是Consumer,consumer不会生成result,所以用null表示无void)
        } catch (Throwable ex) {
            completeThrowable(ex);                                                       // 上游正常完成,当前计算阶段的计算任务执行发生异常,设置当前计算阶段result为异常值
        }
    }
    
    return true;
}


💯💯💯💯💯
注意uniAccept方法是fast-path设置CompletableFuture结果的方法,并没有在其内部调用postComplete()方法

```


## 讲解类的设计

```text
static final int SYNC   =  0;
static final int ASYNC  =  1;
static final int NESTED = -1;

由于级联的原因,同步模式下当前线程不仅执行本计算阶段的计算任务,还同步执行关联的后续所有同步模式的计算任务,以及向提交异步关联的后续所.
```

```text
// Completion对应的CompletableFuture完成时触发的回调,Completion类名改为ActionAfterCompletion更直观
// ForkJoinTask的类型参数是Void, 因为action-after-completion只能执行一次,不需要保留额外的状态
// AsynchronousCompletionTask是marker interface,用于监控.
abstract static class Completion extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
    volatile Completion next;      // Treiber stack link

    /**
     * 💯💯💯Completion的核心方法
     * Performs completion action if triggered, returning a dependent that may need propagation, if one exists.
     * @param mode SYNC(同步模式,在当前线程执行), ASYNC(异步模式,在另一个线程执行), or NESTED(嵌套模式,用于处理内部递归调用)
     * 
     * 当一个 CompletableFuture完成时,它会调用其stack中每个Completion的tryFire方法
     */
    abstract CompletableFuture<?> tryFire(int mode);

    /** Returns true if possibly still triggerable. Used by cleanStack. */
    abstract boolean isLive();

    public final void run()                { tryFire(ASYNC); }                         // 作为Runnable对象,被执行run方法,最终都是异步执行tryFire方法
    public final boolean exec()            { tryFire(ASYNC); return true; }            // 作为ForkJoinTask对象,被执行exec方法,最终都是异步执行tryFire方法
    public final Void getRawResult()       { return null; }
    public final void setRawResult(Void v) {}                                          // 不需要保留额外的状态
}





UniCompletion的uni对应CoCompletion的co.uni表示单个上游完成就会触发下游的完成,co表示需要多个上游的完成才能触发下游完成.

abstract static class UniCompletion<T,V> extends Completion {                     // Completion作为relay对象存在,衔接上游和下游
    Executor executor;                 // dep是异步模式时,用到的线程池
    CompletableFuture<V> dep;          // src的下游计算阶段(下游的计算任务定义在子类中)
    CompletableFuture<T> src;          // 上游计算阶段

    UniCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src) {
        this.executor = executor; this.dep = dep; this.src = src;
    }

    // 因为postFire方法支持协助上游执行postComplete,所以每个UniCompletion在被执行前,需要先被认领该
    final boolean claim() {
        Executor e = executor;
        
        if (compareAndSetForkJoinTaskTag((short)0, (short)1)) {                      // 通过tag-bit来保证认领的并发安全性
            if (e == null)
                return true;                                                         // 只有同步模式才能当前线程认领
            
            executor = null;
            e.execute(this);                                                         // 异步模式下当前线程认领失败 (等价于此时被线程池认领并执行)
        }
        return false;
    }

    final boolean isLive() { return dep != null; }
}







static final class UniAccept<T> extends UniCompletion<T,Void> {
    Consumer<? super T> fn;
    
    UniAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Consumer<? super T> fn) {   // 上游src的下游dep的计算任务是fn
        super(executor, dep, src); 
        this.fn = fn;
    }
    
    final CompletableFuture<Void> tryFire(int mode) {
        CompletableFuture<Void> d; CompletableFuture<T> a;
        
        if ((d = dep) == null || !d.uniAccept(a = src, fn, mode > 0 ? null : this))
            return null;
        
        dep = null; src = null; fn = null;                        // dep设置为null防止fn被多次执行
        return d.postFire(a, mode);
    }
}





static final class UniApply<T,V> extends UniCompletion<T,V> {
    Function<? super T,? extends V> fn;
    
    UniApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T,? extends V> fn) {
        super(executor, dep, src); this.fn = fn;
    }
    
    final CompletableFuture<V> tryFire(int mode) {
        CompletableFuture<V> d; CompletableFuture<T> a;
        
        if ((d = dep) == null || !d.uniApply(a = src, fn, mode > 0 ? null : this))
            return null;
        
        dep = null; src = null; fn = null;
        return d.postFire(a, mode);
    }
}
```


```text
private <V> CompletableFuture<V> uniApplyStage(Executor e, Function<? super T,? extends V> f) {
    if (f == null) throw new NullPointerException();
    
    CompletableFuture<V> d =  new CompletableFuture<V>();            // 创建调用方通过thenApply方法声明的计算阶段(当前计算阶段this是其上游)
    
    // 如果使用同步模式(e==null),则尝试现在是否能完成计算阶段d的计算任务f(只有上游现在已经完成才会尝试成功)
    // 如果是异步模式(e!=null),则由线程池完成计算阶段d的计算任务f
    if (e != null || !d.uniApply(this, f, null)) {
        UniApply<T,V> c = new UniApply<T,V>(e, d, this, f);
        push(c);
        c.tryFire(SYNC);                                              // 防止压入stack与上游完成发生竟态,在此处补一个miss-fire(即使多了一次fire也不用担心,tryFire方法有防护代码最多只能执行一次)
    }
    
    return d;                                                         // 返回调用方声明的计算阶段
}


final <S> boolean uniApply(CompletableFuture<S> a, Function<? super S,? extends T> f, UniApply<S,T> c) {
    Object r; Throwable x;
    if (a == null || (r = a.result) == null || f == null)
        return false;
    
    tryComplete: if (result == null) {
        if (r instanceof AltResult) {
            if ((x = ((AltResult)r).ex) != null) {
                completeThrowable(x, r);
                break tryComplete;
            }
            r = null;
        }
        try {
            if (c != null && !c.claim())
                return false;
            @SuppressWarnings("unchecked") S s = (S) r;
            completeValue(f.apply(s));
        } catch (Throwable ex) {
            completeThrowable(ex);
        }
    }
    
    return true;
}  


thenAccept/thenApply/thenRun底层实现基本相同
```



## thenCombine

```text
// 本方法声明一个计算阶段(即返回值),该计算阶段在this和other都正常完成后完成.声明的计算阶段的计算任务是fn,fn的参数来自于this.resul和other.result
public <U,V> CompletionStage<V> thenCombine (CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn);



public <U,V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,BiFunction<? super T,? super U,? extends V> fn) {
    return biApplyStage(null, other, fn);
}


private <U,V> CompletableFuture<V> biApplyStage(Executor e, CompletionStage<U> o, BiFunction<? super T,? super U,? extends V> f) {
    CompletableFuture<U> b;
    if (f == null || (b = o.toCompletableFuture()) == null)                     // 将o从CompletionStage转换为等价的CompletableFuture
        throw new NullPointerException();
    
    CompletableFuture<V> d = new CompletableFuture<V>();
    if (e != null || !d.biApply(this, b, f, null)) {                           // 尝试fast-path 如果this和o都已经完成就同步执行f
        BiApply<T,U,V> c = new BiApply<T,U,V>(e, d, this, b, f);
        bipush(b, c);
        c.tryFire(SYNC);
    }
    return d;
}


// fast-path设置this的result
// a/b是this的上游,f是this的计算任务,执行f前需要认领c
final <R,S> boolean biApply(CompletableFuture<R> a, CompletableFuture<S> b, BiFunction<? super R,? super S,? extends T> f, BiApply<R,S,T> c) {
    Object r, s; Throwable x;
    if (a == null || (r = a.result) == null ||
        b == null || (s = b.result) == null || f == null)
        return false;
    
    // 如果a/b都已完成,设置this.result
    tryComplete: if (result == null) {
        if (r instanceof AltResult) {
            if ((x = ((AltResult)r).ex) != null) {
                completeThrowable(x, r);
                break tryComplete;
            }
            r = null;
        }
        if (s instanceof AltResult) {
            if ((x = ((AltResult)s).ex) != null) {
                completeThrowable(x, s);
                break tryComplete;
            }
            s = null;
        }
        
        try {
            if (c != null && !c.claim())                         // 防止并发执行f,需要先认领c
                return false;
            
            @SuppressWarnings("unchecked") R rr = (R) r;
            @SuppressWarnings("unchecked") S ss = (S) s;
            completeValue(f.apply(rr, ss));
        } catch (Throwable ex) {
            completeThrowable(ex);
        }
    }
    return true;
}


abstract static class BiCompletion<T,U,V> extends UniCompletion<T,V> {
    CompletableFuture<U> snd; // second source for action
    
    BiCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
        super(executor, dep, src); this.snd = snd;
    }
}
    
static final class BiApply<T,U,V> extends BiCompletion<T,U,V> {
    BiFunction<? super T,? super U,? extends V> fn;
    
    // src/snd为dep的双上游,fn为dep的计算任务
    BiApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd, BiFunction<? super T,? super U,? extends V> fn) {
        super(executor, dep, src, snd); this.fn = fn;
    }
    
    final CompletableFuture<V> tryFire(int mode) {
        CompletableFuture<V> d;
        CompletableFuture<T> a;
        CompletableFuture<U> b;
        
        if ((d = dep) == null ||
            !d.biApply(a = src, b = snd, fn, mode > 0 ? null : this))
            return null;
        
        dep = null; src = null; snd = null; fn = null;
        return d.postFire(a, b, mode);
    }
}


final void bipush(CompletableFuture<?> b, BiCompletion<?,?,?> c) {
    if (c != null) {
        Object r;
        while ((r = result) == null && !tryPushStack(c))                   // 💯将BiCompletion放入this.stack中
            lazySetNext(c, null); // clear on failure
        
        if (b != null && b != this && b.result == null) {
            Completion q = (r != null) ? c : new CoCompletion(c);          // 💯如果this已经完成则将BiCompletion放入另一个stack中; 如果this未完成则将CoCompletion放入另一个stack中
            while (b.result == null && !b.tryPushStack(q))
                lazySetNext(q, null); // clear on failure
        }
    }
}






static final class CoCompletion extends Completion {
    BiCompletion<?,?,?> base;
    
    CoCompletion(BiCompletion<?,?,?> base) { this.base = base; }
    
    
    final CompletableFuture<?> tryFire(int mode) {
        BiCompletion<?,?,?> c; CompletableFuture<?> d;
        
        if ((c = base) == null || (d = c.tryFire(mode)) == null)     // CoCompletion只提供链接作用,核心还是BiCompletion. BiCompletion完成了(tryFire返回null)也就CoCompletion完成了
            return null;
        
        base = null; // 防止重复执行
        return d;
    }
    
    
    final boolean isLive() {
        BiCompletion<?,?,?> c;
        return (c = base) != null && c.dep != null;    // this.dep和base.dep有一方为null表示已经有线程正在执行
    }
}
```



```text
// fn的返回值是中间计算阶段m,m是当前计算阶段this的下游,thenCompose的返回值是m的下游n.中间计算阶段m完成后触发n完成且将m.result设置为n.result
// fn以当前计算阶段this的result为入参.fn并不是任何计算阶段的计算任务,而是当前计算阶段this的一个纯粹的completion-action. (多数的计算阶段的计算任务拥有两面性,一面是上游的completion-action,另一面是下游的计算任务)🚀🚀🚀
// fn动态声明了用户不可见的中间计算阶段m,fn要保证m一定会被触发(通过向线程池提交异步任务获得m/通过构造已完成的m/通过thenXXX获得m)🚀🚀🚀
public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
    return uniComposeStage(null, fn);
}



private <V> CompletableFuture<V> uniComposeStage(Executor e, Function<? super T, ? extends CompletionStage<V>> f) {
    if (f == null) throw new NullPointerException();
    Object r; Throwable x;
    
    // 声明同步模式的下游,且当前计算阶段已经完成, 直接返回用户一个完成的下游
    if (e == null && (r = result) != null) {
        if (r instanceof AltResult) {
            if ((x = ((AltResult)r).ex) != null) {
                return new CompletableFuture<V>(encodeThrowable(x, r));                    // 返回一个异常完成的下游
            }
            r = null;
        }
        
        try {
            @SuppressWarnings("unchecked") T t = (T) r;
            CompletableFuture<V> g = f.apply(t).toCompletableFuture();
            Object s = g.result;
            if (s != null)
                return new CompletableFuture<V>(encodeRelay(s));                           // fn
            
            CompletableFuture<V> d = new CompletableFuture<V>();
            UniRelay<V> copy = new UniRelay<V>(d, g);
            g.push(copy);
            copy.tryFire(SYNC);
            return d;
        } catch (Throwable ex) {
            return new CompletableFuture<V>(encodeThrowable(ex));
        }
    }
    
    
    CompletableFuture<V> d = new CompletableFuture<V>();
    UniCompose<T,V> c = new UniCompose<T,V>(e, d, this, f);
    push(c);
    c.tryFire(SYNC);
    return d;
}





final <S> boolean uniCompose(CompletableFuture<S> a, Function<? super S, ? extends CompletionStage<T>> f, UniCompose<S,T> c) {
    Object r; Throwable x;
    if (a == null || (r = a.result) == null || f == null)
        return false;
    tryComplete: if (result == null) {
        if (r instanceof AltResult) {
            if ((x = ((AltResult)r).ex) != null) {
                completeThrowable(x, r);
                break tryComplete;
            }
            r = null;
        }
        try {
            if (c != null && !c.claim())
                return false;
            
            
            @SuppressWarnings("unchecked") S s = (S) r;
            CompletableFuture<T> g = f.apply(s).toCompletableFuture();          // 🚀🚀🚀💯💯💯 中间计算阶段g在f.apply返回前一定要提交到异步线程池/或者成为其他计算阶段的下游/或者已经完成,否则将永远无法被触发完成
            if (g.result == null || !uniRelay(g)) {
                UniRelay<T> copy = new UniRelay<T>(this, g);
                g.push(copy);                                                   // 💯💯💯中间计算阶段g来触发返回给用户的计算阶段,并且将中间阶段阶段的m.result设置为用户的计算阶段的result
                copy.tryFire(SYNC);
                if (result == null)
                    return false;
            }
        } catch (Throwable ex) {
            completeThrowable(ex);
        }
    }
    return true;
}
```


```text
构建一个二叉树,根节点是用户声明的计算阶段.叶子节点是cfs数组的元素.中间节点是relay性质的计算阶段.
在整体树形构建好之前,子树一旦构建好就去尝试执行触发中间节点的完成(理想状态下,这里可以避免构造不必要的部分子树).


static CompletableFuture<Object> orTree(CompletableFuture<?>[] cfs, int lo, int hi) {
    
    // d是区间元素构建的树的根节点(区间内任何一个cfs元素的完成都会触发根节点的完成)
    CompletableFuture<Object> d = new CompletableFuture<Object>();
    
    if (lo <= hi) {
        CompletableFuture<?> a, b;   // a/b分别代表根节点d的左右子树(即左右子树的根节点)
        
        int mid = (lo + hi) >>> 1;
        // 递归的终结区间是: 区间内只有1个或2个元素,否则一直下钻递归
        if ( (a = (lo == mid ? cfs[lo] : orTree(cfs, lo, mid))) == null ||                              
             (b = (lo == hi ? a : (hi == mid+1) ? cfs[hi] : orTree(cfs, mid+1, hi)))  == null )
            throw new NullPointerException();                                                                   // a==null和b==null是防御代码
        
        
        // 在整体树形构建好之前,子树一旦构建好就去尝试执行relay性质的中间节点
        // 初始,a/b子树指向cfs元素; 然后,a/b子树一个是中间节点,一个是cfs元素;最后,a/b子树都是中间节点
        if (!d.orRelay(a, b)) {                                                                                //  左右子树任何一个完成都会触发中间节点的完成            
            OrRelay<?,?> c = new OrRelay<>(d, a, b);
            a.orpush(b, c);
            c.tryFire(SYNC);
        }
    }
    
    return d;  // 返回用户声明的计算阶段
}


if (lo <= hi)
正常递归终止条件: 若区间非法,直接返回一个永远不完成的future(极少见)




(a = (lo == mid ? cfs[lo] : orTree(cfs, lo, mid)))  
lo == mid,意味着区间[lo, hi]有1个或2个元素,lo==hi/或者lo==hi-1 (此时触发递归的结束)

(b = (lo == hi ? a : (hi == mid+1) ? cfs[hi] : orTree(cfs, mid+1, hi))) 中lo == hi ?用来进一步确认区间中有一个还是两个元素
lo == hi,意味着区间[lo, hi]只有一个元素,lo==hi
hi == mid+1,意味着区间[lo, hi]只有2个元素,lo+1==hi


整体执行模型💯💯💯
假设4个future: f0  f1  f2  f3
构建出的 OR 树：
     d
    / \
  o01  o23
  / \  / \
f0 f1 f2 f3
任何叶子完成 → 向上relay → d完成



final void orpush(CompletableFuture<?> b, BiCompletion<?,?,?> c) {
    if (c != null) {
        
        // while循环是为了实现tryPushStack失败时发起重试操作.
        // 先将c作为当前计算阶段的completion-action,再将c作为计算阶段b的completion-action(c被包装成CoCompletion)
        while ((b == null || b.result == null) && result == null) {                                           // (b == null)为防御性代码,主要是为了使用(b.result == null)
            if (tryPushStack(c)) {
                if (b != null && b != this && b.result == null) {
                    Completion q = new CoCompletion(c);
                    while (result == null && b.result == null && !b.tryPushStack(q))
                        lazySetNext(q, null); // clear on failure
                }
                break;
            }
            lazySetNext(c, null); // clear on failure
        }
    }
}



final boolean orRelay(CompletableFuture<?> a, CompletableFuture<?> b) {
    Object r;
    if (a == null || b == null ||                                   // 防御性代码: a == null || b == null
        ((r = a.result) == null && (r = b.result) == null))
        return false;
    if (result == null)
        completeRelay(r);
    return true;
}
```


```text
static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs, int lo, int hi) {
    CompletableFuture<Void> d = new CompletableFuture<Void>();
    
    // 不同于orTree,andTree对于空区间,返回一个完成的节点. 这是为了满足逻辑恒等式 AND(∅) = true
    if (lo > hi) // empty 
        d.result = NIL;
    else {
        CompletableFuture<?> a, b;
        int mid = (lo + hi) >>> 1;
        if ( (a = (lo == mid ? cfs[lo] : andTree(cfs, lo, mid))) == null ||
             (b = (lo == hi ? a : (hi == mid+1) ? cfs[hi] : andTree(cfs, mid+1, hi)))  == null )
            throw new NullPointerException();
        
        if (!d.biRelay(a, b)) {                                  // 左右子树都完成才会触发中间节点的完成
            BiRelay<?,?> c = new BiRelay<>(d, a, b);
            a.bipush(b, c);
            c.tryFire(SYNC);
        }
    }
    
    return d;
}
```



```text
# 简单类型的input-output

根据头阶段计算任务的input-output来划分计算任务的类型:
----------------------------------------------------------------------------- 
input  output    计算任务的类型        对应方法名
----------------------------------------------------------------------------- 
 ×       ×        Runnable          static CompletableFuture.runAsync      (计算阶段链的头阶段没有上游,所以是static类方法而非实例方法)
 √       ×        N/A               头阶段无上游
 ×       √        Supplier          static CompletableFuture.supplyAsync   
 √       √        N/A               头阶段无上游
----------------------------------------------------------------------------- 
 
 
根据非头计算阶段的计算任务的input-output来划分计算任务的类型:
----------------------------------------------------------------------------- 
input  output    计算任务的类型        对应方法名
----------------------------------------------------------------------------- 
 ×       ×        Runnable          CompletionStage.thenRun(Async)
 √       ×        Consumer          CompletionStage.thenAccept(Async)
 ×       √        Function          上游无result,或无视input
 √       √        Function          CompletionStage.thenApply(Async)
----------------------------------------------------------------------------- 

备注:
1.output值会被当作计算阶段的result.
2.无output的,计算阶段的result为特殊的null对象(用AltResult对象区别于默认值null).
3.input值指的是上游计算阶段的result.


# 多值input



# 复杂类型的input-output(input/output是CompletionStage类型)
```


