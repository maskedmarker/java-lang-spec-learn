# juc-AQS-CountDownLatch

AQS.state的含义:
state表示剩余计数


CountDownLatch不支持Lock接口也不支持Condition接口,但是自己却实现了一个类似与Condition的await方法.

CountDownLatch.Sync在共享模式下工作.


## 使用样例
```text
class Driver { 
  void main() throws InterruptedException {
    CountDownLatch startSignal = new CountDownLatch(1);
    CountDownLatch doneSignal = new CountDownLatch(N);

    for (int i = 0; i < N; ++i) {
      new Thread(new Worker(startSignal, doneSignal)).start();
    }

    doSomethingElse();            // don't let run yet
    startSignal.countDown();      // let all threads proceed
    doSomethingElse();
    doneSignal.await();           // wait for all to finish
  }
}

class Worker implements Runnable {
  private final CountDownLatch startSignal;
  private final CountDownLatch doneSignal;
  Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
    this.startSignal = startSignal;
    this.doneSignal = doneSignal;
  }
  public void run() {
    try {
      startSignal.await();
      doWork();
      doneSignal.countDown();
    } catch (InterruptedException ex) {} // return;
  }

  void doWork() { ... }
}
```

## 源码实现

AQS的acquire类方法和release类方法都会引起AQS的状态变化,但最主要的不同体现在
acquire类方法可能会将线程挂起等待,而release类方法永远不会挂起线程.

CountDownLatch.await在latch未达到0时阻塞调用方,所以必须使用acquire类的方法;
而CountDownLatch.countDown不能阻塞调用方,所以必须使用release类的方法;
而CountDownLatch支持多个线程同时调用,所以使用share-mode

```text
public class CountDownLatch {
    private final Sync sync;
    
    public void countDown() {
        sync.releaseShared(1);
    }
    
    public void await() throws InterruptedException {
        // AQS的acquire会因条件不满足而挂起等待,所在CountDownLatch.await这里只能使用acquire类的方法;那么所在CountDownLatch.countDown那里只能使用release类的方法
        sync.acquireSharedInterruptibly(1);
    }
    
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }
}
```

### Sync
```text
private static final class Sync extends AbstractQueuedSynchronizer {

    Sync(int count) {
        setState(count);
    }

    int getCount() {
        return getState();
    }
    
    protected int tryAcquireShared(int acquires) {
        // 该方法不用来抢锁资源,仅仅用来判断状态是否达到0.入参acquires无意义忽略.
        
        // 当剩余计数为0时,才不用等待
        return (getState() == 0) ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        // 存在并发countDown,所以cas需要重试
        for (;;) {
            int c = getState();
            // state变为0后,不再需要唤醒线程.
            if (c == 0)
                return false;
            
            // 设计者仅要求从1变为0的那个操作唤醒等待的线程.
            int nextc = c-1;
            if (compareAndSetState(c, nextc))
                return nextc == 0;
        }
    }
}

由于共享模式的级联唤醒,只需要一个唤醒(state从1变为0)就能将所有已经等待的都唤醒(此时getState() == 0).
state变为0后,不再需要唤醒线程.是因为此时(getState() == 0),tryAcquireShared不会再导致线程挂起.
```