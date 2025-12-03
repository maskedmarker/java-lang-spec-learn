# juc-AQS-Semaphore

Semaphore不支持Lock接口,也就不支持Condition.
所以调用release方法时不想其他的Lock,不需要当前线程持有锁.

release就是增加一个permit,然后唤醒等待的线程.
acquire就是试图减少一个permit,如果permit<=0则挂起等待.

Semaphore可以设置初始permit个数.如果初始permit个数为0,首个acquire会一直阻塞到第一个release向Semaphore中增加permit.

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
    
    
    abstract static class Sync extends AbstractQueuedSynchronizer {
        
        // ...
        
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        protected final boolean tryReleaseShared(int releases) {
            // 通过重试完成cas,为state增加permit
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
    
    static final class NonfairSync extends Sync {

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }
    
    
    static final class FairSync extends Sync {

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            for (;;) {
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
}

(remaining < 0 || compareAndSetState(available, remaining)) 这个写法非常好,等价于 (remaining < 0 || (remaining >= 0 && compareAndSetState(available, remaining) == true)), 可以省略掉(remaining >= 0)
即如果remaining小于0 或者当remaining大于等于0时cas-state成功
```
