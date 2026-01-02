# juc-CyclicBarrier

CyclicBarriers are used in programs in which we have a fixed number of threads that must wait for each other to reach a common point before continuing execution.
The barrier is called cyclic because it can be re-used after the waiting threads are released.


## 使用样例


## 源码实现

### dowait

如果没有发生超时/中断,
当非最后一个到达的线程调用await方法会触发当前线程挂起;
最后一个到达的线程会先触发barrierAction,然后唤醒其他挂起的线程,更新CyclicBarrier的generation,然后释放锁并退出CyclicBarrier.await方法;
被唤醒的线程依次获取到锁后,发现generation变了,然后释放锁并退出CyclicBarrier.await方法.


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
                
                nextGeneration();  // 💯💯💯先执行barrierCommand,然后再触发nextGeneration
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
