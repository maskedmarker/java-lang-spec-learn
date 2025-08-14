# juc-thread-pool-1

在Java并发编程中，ThreadPoolExecutor 使用状态机管理其生命周期，状态通过原子变量 ctl（高3位表示状态，低29位表示工作线程数）维护。
状态变化是单向的

```text
ThreadPoolExecutor使用一个AtomicInteger（ctl）来同时表示线程池的状态（runState）和有效线程数（workerCount）。其中高3位表示状态，低29位表示线程数量。

ThreadPoolExecutor定义了5种状态：
1. RUNNING:  接受新任务并处理队列中的任务。
   - 状态值：高3位为111（十进制为-536870912）
   - 状态值（十进制）：-536870912
   - 二进制表示（32位）：11100000 00000000 00000000 00000000（实际存储的是补码，这里用原码示意）
2. SHUTDOWN: 不接受新任务，但处理队列中的任务。
   - 状态值：高3位为000（十进制为0）
   - 状态值（十进制）：0
3. STOP:  不接受新任务，不处理队列中的任务，并中断正在执行的任务。
   - 状态值：高3位为001（十进制为536870912）
   - 状态值（十进制）：536870912
4. TIDYING:  所有任务已经终止，workerCount为0，线程转换到TIDYING状态将会运行terminated()钩子方法。
   - 状态值：高3位为010（十进制为1073741824）
   - 状态值（十进制）：1073741824
5. TERMINATED: terminated()方法已经完成。
   - 状态值：高3位为011（十进制为1610612736）
   - 状态值（十进制）：1610612736

状态转换：
1. 初始状态为RUNNING。
2. 当调用shutdown()方法时，状态从RUNNING变为SHUTDOWN。此时不再接受新任务，但会继续处理队列中的任务。
3. 当调用shutdownNow()方法时，状态从RUNNING（或SHUTDOWN）直接变为STOP。注意：如果当前是SHUTDOWN状态，调用shutdownNow()也会变为STOP。
   - 但是，根据源码，实际上shutdownNow()只能在RUNNING或SHUTDOWN状态下调用。如果已经是STOP或以上状态，调用shutdownNow()不会有状态变化。
4. 当线程池和任务队列都为空（即workerCount为0且任务队列为空）时，在SHUTDOWN状态下会转换到TIDYING状态。
5. 在STOP状态下，当workerCount变为0时，会转换到TIDYING状态。
6. 当进入TIDYING状态后，会执行terminated()方法（该方法为空，可以被子类覆盖）。执行完毕后，状态变为TERMINATED。
注意：状态转换图如下（箭头表示转换方向）：
  RUNNING -> SHUTDOWN   （调用shutdown()）
  (RUNNING or SHUTDOWN) -> STOP （调用shutdownNow()）
  SHUTDOWN -> TIDYING   （当队列和线程池都为空）
  STOP -> TIDYING       （当线程池为空）
  TIDYING -> TERMINATED （terminated()执行完毕）
具体转换条件：
- RUNNING -> SHUTDOWN: 显式调用shutdown()，或者在finalize()中隐式调用。
- (RUNNING or SHUTDOWN) -> STOP: 显式调用shutdownNow()。
- SHUTDOWN -> TIDYING: 当任务队列为空并且线程池中工作线程数量为0时，自动转换。
- STOP -> TIDYING: 当线程池中工作线程数量为0时，自动转换。
- TIDYING -> TERMINATED: 当terminated()方法执行完毕。
状态转换的代码逻辑主要在ThreadPoolExecutor的runWorker()、processWorkerExit()、tryTerminate()等方法中。

下面我们详细说明tryTerminate()方法中的状态转换逻辑：
tryTerminate()方法在可能终止线程池时被调用（例如，工作线程退出时）。在该方法中：
1. 如果线程池状态是RUNNING，则直接返回（不终止）。
2. 如果状态是SHUTDOWN且任务队列不为空，则返回（还需要处理队列中的任务）。
3. 如果当前工作线程数不为0，则尝试中断一个空闲线程（通过调用interruptIdleWorkers(ONLY_ONE)），然后返回。
4. 如果状态是SHUTDOWN（任务队列已空）或STOP，并且工作线程数为0，则尝试将状态转换为TIDYING，然后执行terminated()方法，最后将状态设置为TERMINATED。
在tryTerminate()中，状态转换到TIDYING的条件是：状态为SHUTDOWN或STOP，且工作线程数为0，并且任务队列为空（对于SHUTDOWN状态）或者不需要考虑队列（对于STOP状态，因为STOP状态已经清空队列了）。
另外，在processWorkerExit()方法中，当一个工作线程退出时会调用tryTerminate()。


总结：
- 状态变化是单向的：RUNNING -> SHUTDOWN -> TIDYING -> TERMINATED 或者 RUNNING -> STOP -> TIDYING -> TERMINATED。
- 状态转换条件严格，且通过CAS操作确保原子性。
- 状态转换到TIDYING和TERMINATED时，线程池已经没有任何工作线程，且不再处理任何任务。
```

状态转换核心
```text
final void tryTerminate() {
    for (;;) {
        int c = ctl.get();
        // 条件1：RUNNING 状态不终止
        // 条件2：SHUTDOWN 但队列非空需继续处理
        if (isRunning(c) || 
            runStateAtLeast(c, TIDYING) || 
            (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
            return;
        }
        
        // 工作线程未完全退出 → 中断一个空闲线程
        if (workerCountOf(c) != 0) {
            interruptIdleWorkers(ONLY_ONE);
            return;
        }

        // 满足转换条件：CAS 更新为 TIDYING
        if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
            try {
                terminated(); // 执行钩子方法
            } finally {
                ctl.set(ctlOf(TERMINATED, 0)); // 更新为 TERMINATED
                termination.signalAll(); // 唤醒等待线程
            }
            return;
        }
    }
}
```

状态转换图

```text
          shutdown()
   RUNNING ────────────▶ SHUTDOWN
      │                     │
      │ shutdownNow()       │ 队列空 & 线程数=0
      ▼                     ▼
    STOP ◀──────────────────┤
      │                     │
      │ 线程数=0             │
      ▼                     ▼
    TIDYING ──────────────▶ TIDYING
      │
      │ terminated()
      ▼
  TERMINATED
```

状态转换流程
```text
1. RUNNING → SHUTDOWN
触发条件：调用 shutdown() 方法。
行为变化：
    停止接受新任务（execute() 会拒绝新任务）。
    继续处理阻塞队列中的剩余任务。
    中断所有空闲线程（通过 getTask() 阻塞的线程）。

2. RUNNING/SHUTDOWN → STOP
触发条件：调用 shutdownNow() 方法。
行为变化：
    立即停止接受新任务。
    清空阻塞队列（返回未处理的任务列表）。
    中断所有工作线程（无论是否空闲）。

3. SHUTDOWN → TIDYING
触发条件：
    阻塞队列为空。
    所有工作线程已退出（workerCount = 0）。
关键方法：tryTerminate() 检测条件并触发转换。

4. STOP → TIDYING
触发条件：所有工作线程已退出（workerCount = 0）。
说明：STOP 状态已清空队列，只需等待线程终止。

5. TIDYING → TERMINATED
触发条件：执行完钩子方法 terminated()。
行为：
    状态自动转换（无外部触发）。
    terminated() 默认为空方法，可被子类重写（如资源清理）。
```
