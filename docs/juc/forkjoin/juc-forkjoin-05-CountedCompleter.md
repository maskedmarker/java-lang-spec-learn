# juc-forkjoin-CountedCompleter


RecursiveAction/RecursiveTask是通过fork-join这样的固定的结构化范式来切分出子任务/等待子任务的完成并手动合并子任务结果;而CountedCompleter通过pending计数器来表示未完成的子任务数量,没有固定的范式.
fork-join这样固定的结构化范式是无法改动的;而计数器是可以方便操控,所以有了直接手动设置子任务/根任务完成.
fork-join利用join方法的调用来隐式表明夫与子的关系,以及join方法return来隐式表明任务的完成,而CountedCompleter中使用显示的completer属性表达父子任务,以及显示的pending属性(未完成子任务数)+显示声明任务完成(即调用tryComplete/propagateCompletion)来处理任务的完成.


CountedCompleter behaves differently because it needs the explicit call of tryComplete() or propagateCompletion() before returning. Otherwise, the task is considered as not completed and it runs indeterminately.

```text
为什么选择 CountedCompleter 而不是 RecursiveTask？

内存优化：CountedCompleter在任务执行完后可以立即释放资源,不需要保留栈信息等待join().
非阻塞：它是为那些任务之间没有直接返回值依赖、只需要知道“何时全部结束”的场景设计的.
```

## CountedCompleter

```text
public abstract class CountedCompleter<T> extends ForkJoinTask<T> {

    // 夫任务
    final CountedCompleter<?> completer;
    volatile int pending;

    protected final boolean exec() {
        compute();
        return false;  // 强制返回false,这样ForkJoinTask就不会setCompletion(NORMAL),而由compute中的tryComplete完成设置setCompletion(NORMAL)
    }
    
    // 预留给用户定义计算逻辑(从ForkJoinTask.exec转移到了CountedCompleter.compute)
    // 💯💯💯 这里预设了每个任务都会在compute中调用tryComplete(当前任务和子任务都要调用. 因为CountedCompleter将判断整体任务是否完成的逻辑封装起来,每个任务在完成后都要通过tryComplete告知CountedCompleter框架,由其来定夺)
    public abstract void compute();
    
    
    // 默认返回null,如果需要返回值,需要用户override该方法
    public T getRawResult() { return null; }
    
    // Performs an action when method tryComplete is invoked and the pending count is zero, or when the unconditional method complete is invoked. 
    public void onCompletion(CountedCompleter<?> caller) {
        // 用户可以在该方法合并子任务结果(当前任务的最后一次(💯💯💯而非每次)tryComplete才会触发onCompletion的执行,同时也意味着onCompletion只会执行一次,所以无并发问题)
        // onCompletion的入参caller仅仅表示导致任务完成的最后一位贡献者(任务本体及其子任务都是贡献者)
    }
    
    
    // 父子任务构成一棵树
    public final CountedCompleter<?> getRoot() {
        CountedCompleter<?> a = this, p;
        while ((p = a.completer) != null)
            a = p;
        return a;
    }
    
    // 还有一些手动控制的final方法,比RecursiveAction/RecursiveTask更加灵活
}
```


## tryComplete

tryComplete充当任务complete事件在父子任务链上传播.

父子任务构成一棵树.
叶子子任务pending初始值为0,tryComplete时,触发onCompletion,并顺带父任务的未完成子任务数减一.
中间任务的pending初始值为大于零的N,其前N次的tryComplete都仅仅是未完成子任务数减一,第N+1次的tryComplete才会触发onCompletion,并顺带其父任务的未完成子任务数减一.
根任务的pending初始值为大于零的M,其前M次的tryComplete都仅仅是未完成子任务数减一,第M+1次的tryComplete才会触发onCompletion,并顺带标记根任务完成(进而唤醒等待根任务结果的线程).

N+1/M+1次是因为当前任务和子任务都要调用tryComplete

备注: 子任务是不会被标记任务完成,只有根任务会被标记任务完成.

```text
public final void tryComplete() {
    // 变量a指向循环中正在处理的任务,初始a指向当前任务(this),后续循环a会成为变为a的父任务(.completer)
    CountedCompleter<?> a = this, s = a;
    
    // 未完成子任务数减一(pending--),且如果子任务都完成则执行onCompletion;
    
    for (int c;;) {
        if ((c = a.pending) == 0) {
            a.onCompletion(s);       // 当a是叶子任务时,a==s;当a是非叶子任务时,a是s的父任务. a.onCompletion(s)表达的是由于任务s的完成触发了任务a的完成.  💯💯💯当前任务的最后一次tryComplete才会触发onCompletion的执行
            if ((a = (s = a).completer) == null) {    // 此时变量a指向其父任务
                s.quietlyComplete();      // (a.completer==null)此时变量a指向根任务,且根任务的子任务都已完成(a.pending==0),这时才能标记根任务完成. 💯💯💯子任务是不会被标记任务完成
                return;
            }
        }
        else if (U.compareAndSwapInt(a, PENDING, c, c - 1))  // 未完成子任务数减一(如果cas失败下个循环重试,本循环变量a没有改变)
            return;
    }
}
```
```text
父子任务的“完成传播链”

child.tryComplete()
    ↓
child.pending == 0
    ↓
child.onCompletion()
    ↓
parent.tryComplete()
    ↓
parent.pending == 0 ?

👉 完成是 向上传播的
```

## propagateCompletion

Equivalent to tryComplete but does not invoke onCompletion(CountedCompleter) along the completion path:
If the pending count is nonzero, decrements the count; otherwise, similarly tries to complete this task's completer, if one exists, else marks this task as complete.
This method may be useful in cases where onCompletion should not, or need not, be invoked for each completer in a computation.

```text
public final void propagateCompletion() {
    CountedCompleter<?> a = this, s = a;
    for (int c;;) {
        if ((c = a.pending) == 0) {
            if ((a = (s = a).completer) == null) {
                s.quietlyComplete();
                return;
            }
        }
        else if (U.compareAndSwapInt(a, PENDING, c, c - 1))
            return;
    }
}
```

## complete

手动设置(子)任务完成

```text
public void complete(T rawResult) {
    CountedCompleter<?> p;
    setRawResult(rawResult);
    onCompletion(this);
    quietlyComplete();
    if ((p = completer) != null)
        p.tryComplete();
}
```

## quietlyCompleteRoot

手动设置根任务完成
Equivalent to getRoot().quietlyComplete().

```text
public final void quietlyCompleteRoot() {
    for (CountedCompleter<?> a = this, p;;) {
        if ((p = a.completer) == null) {
            a.quietlyComplete();
            return;
        }
        a = p;
    }
}
```
