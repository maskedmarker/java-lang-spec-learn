# juc-CyclicBarrier

## 使用样例


## 源码实现

### dowait

```text
private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException, TimeoutException {
    final ReentrantLock lock = this.lock;
    
    lock.lock();
    try {
        final Generation g = generation;

        if (g.broken)
            throw new BrokenBarrierException();

        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }

        int index = --count;
        if (index == 0) {  // tripped
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                nextGeneration();
                return 0;
            } finally {
                if (!ranAction)
                    breakBarrier();
            }
        }

        // loop until tripped, broken, interrupted, or timed out
        for (;;) {
            try {
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {                // 内部的trip.await消费了中断,为了让CyclicBarrier使用方感知到其他线程的中断操作,要不rethrow-InterruptedException,要么补充中断标识
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {                                       // 能进入当前分支,意味着g!=generation或者g.broken=true,后面都将结束CyclicBarrier.await方法
                    Thread.currentThread().interrupt();        // 补充中断标识
                }
            }

            if (g.broken)
                throw new BrokenBarrierException();

            if (g != generation)
                return index;

            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
```
