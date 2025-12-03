# juc-AQS-Semaphore

AQS.state的含义:
state表示剩余许可数量


Semaphore不支持Lock接口,也就不支持Condition.
所以调用release方法时不想其他的Lock,不需要当前线程持有锁.

release就是增加一个permit,然后唤醒等待的线程.
acquire就是试图减少一个permit,如果permit<=0则挂起等待.

Semaphore可以设置初始permit个数.如果初始permit个数为0,首个acquire会一直阻塞到第一个release向Semaphore中增加permit.

## 源码实现

```text
// Semaphore不支持Lock接口,也就不支持Condition
public class Semaphore implements java.io.Serializable {
    private final Sync sync;
    
    // 设置初始permit的个数
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }
    
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
    
    public void release() {
        sync.releaseShared(1);
    }
}
```

### Sync

```text
abstract static class Sync extends AbstractQueuedSynchronizer {
    
    // ...
    
    final int nonfairTryAcquireShared(int acquires) {
        // 💯 只要还有permit,可以多尝试几次 (当然这里也可以不用循环就cas-state一次,失败了先挂起然后通过共享模式的级联唤醒)
        for (;;) {
            int available = getState();
            int remaining = available - acquires;
            
            // 为了非公平性,当(remaining >= 0)时,不考虑同步队列中的排队者,直接尝试抢占锁
            if (remaining < 0 ||
                compareAndSetState(available, remaining)) // cas-state时,新值不能是负数
                return remaining;
        }
    }

    protected final boolean tryReleaseShared(int releases) {
        // 💯 因为存在并发release,所以需要通过重试完成cas增加permit
        for (;;) {
            int current = getState();
            int next = current + releases;
            if (next < current) // overflow
                throw new Error("Maximum permit count exceeded");
            if (compareAndSetState(current, next))
                return true;
        }
    }
    
    // ...
}

(remaining < 0 || compareAndSetState(available, remaining)) 这个写法非常好,等价于 (remaining < 0 || (remaining >= 0 && compareAndSetState(available, remaining) == true)), 可以省略掉(remaining >= 0)
即如果remaining小于0 或者当remaining大于等于0且cas-state成功
```

### FairSync
```text
static final class FairSync extends Sync {

    FairSync(int permits) {
        super(permits);
    }

    protected int tryAcquireShared(int acquires) {
        // 💯只要还有permit,可以多尝试几次
        for (;;) {
            // 为了FIFO的公平性,如果同步队列中有更早的排队者,直接放弃尝试,等待依次被唤醒后再去抢占锁
            if (hasQueuedPredecessors())
                return -1;
            
            int available = getState();
            int remaining = available - acquires;
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }
}
```


### NonfairSync

```text
static final class NonfairSync extends Sync {

    NonfairSync(int permits) {
        super(permits);
    }

    protected int tryAcquireShared(int acquires) {
        return nonfairTryAcquireShared(acquires);
    }
}
```