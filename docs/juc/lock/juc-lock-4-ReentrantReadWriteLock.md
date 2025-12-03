# juc-AQS-ReentrantReadWriteLock

## 使用样例


## 源码实现

```text
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {

    private final ReentrantReadWriteLock.ReadLock readerLock;
    private final ReentrantReadWriteLock.WriteLock writerLock;
    
    // ReadLock和WriteLock共用ReentrantReadWriteLock的sync
    final Sync sync;
    
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

}
```

### ReadLock(共享模式)

```text
public static class ReadLock implements Lock, java.io.Serializable {
    private final Sync sync;
    
    protected ReadLock(ReentrantReadWriteLock lock) {
        // ReadLock和WriteLock共用ReentrantReadWriteLock的sync
        sync = lock.sync;
    }
    
    public void lock() {
        // 💯使用共享模式
        sync.acquireShared(1);
    }
    
    public boolean tryLock() {
        return sync.tryReadLock();
    }
    
    public void unlock() {
        // 💯使用共享模式
        sync.releaseShared(1);
    }
    
    // 💯不支持Condition(Condition是为独占模式设计的)
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
```

### WriteLock(独占模式)
```text
public static class WriteLock implements Lock, java.io.Serializable {
    private final Sync sync;
    
    protected WriteLock(ReentrantReadWriteLock lock) {
        // ReadLock和WriteLock共用ReentrantReadWriteLock的sync
        sync = lock.sync;
    }
    
    public void lock() {
        // 💯使用独占模式
        sync.acquire(1);
    }
    
    public boolean tryLock() {
        return sync.tryWriteLock();
    }
    
    public void unlock() {
        // 💯使用独占模式
        sync.release(1);
    }
    
    // 💯支持Condition(Condition是为独占模式设计的)
    public Condition newCondition() {
        return sync.newCondition();
    }
}
```

### 辅助类
```text
static final class HoldCounter {
    
    int count;          // initially 0
    
    // Use id, not reference, to avoid garbage retention
    final long tid = LockSupport.getThreadId(Thread.currentThread());
}


        
static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
    
    public HoldCounter initialValue() {
        return new HoldCounter();
    }
}
```


### Sync
```text
abstract static class Sync extends AbstractQueuedSynchronizer {

    // Lock state is logically divided into two unsigned shorts: The lower one representing the exclusive (writer) lock hold count, and the upper the shared (reader) hold count.

    Sync() {
        readHolds = new ThreadLocalHoldCounter();
        setState(getState()); // ensures visibility of readHolds
    }
    
    protected final boolean tryAcquire(int acquires) {
        /*
         * Walkthrough:
         * 1. If read count nonzero or write count nonzero and owner is a different thread, fail.
         * 2. If count would saturate, fail. (This can only happen if count is already nonzero.)
         * 3. Otherwise, this thread is eligible for lock if
         *    it is either a reentrant acquire or queue policy allows it. If so, update state and set owner.
         */
        Thread current = Thread.currentThread();
        int c = getState();
        int w = exclusiveCount(c);
        
        // 如果c!=0且w==0则有reader持有读锁,当前线程尝试抢占写锁失败;
        // 如果c!=0且w!=0则可能有reader/writer,若当前线程是owner则有且仅有当前线程以writer或reader已经获取了锁(同一个线程无法并发读写操作,此时不用区分读写),否则有其他线程的reader/writer需要挂起等待写锁;
        if (c != 0) {
            // (Note: if c != 0 and w == 0 then shared count != 0)
            if (w == 0 || current != getExclusiveOwnerThread())
                return false;
            if (w + exclusiveCount(acquires) > MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
            setState(c + acquires); // Reentrant acquire
            return true;
        }
        
        // 如果c==0,仅表示没有reader/writer持有锁,还要判断队列中是否有排队的:如果已经有其他线程在排队了,当前线程尝试抢占写锁也是失败的;如果没有其他线程排队,这时才用cas-state来尝试.
        前面是否有可以直接去尝试抢占写锁; 
        if (writerShouldBlock() ||
            !compareAndSetState(c, c + acquires))
            return false;
        setExclusiveOwnerThread(current);
        return true;
    }
    
    protected final boolean tryRelease(int releases) {
        if (!isHeldExclusively())
            throw new IllegalMonitorStateException();
        int nextc = getState() - releases;
        boolean free = exclusiveCount(nextc) == 0;
        if (free)
            setExclusiveOwnerThread(null);
        setState(nextc);
        return free;
    }
    
    protected final boolean tryReleaseShared(int unused) {
        Thread current = Thread.currentThread();
        if (firstReader == current) {
            // assert firstReaderHoldCount > 0;
            if (firstReaderHoldCount == 1)
                firstReader = null;
            else
                firstReaderHoldCount--;
        } else {
            HoldCounter rh = cachedHoldCounter;
            if (rh == null ||
                rh.tid != LockSupport.getThreadId(current))
                rh = readHolds.get();
            int count = rh.count;
            if (count <= 1) {
                readHolds.remove();
                if (count <= 0)
                    throw unmatchedUnlockException();
            }
            --rh.count;
        }
        for (;;) {
            int c = getState();
            int nextc = c - SHARED_UNIT;
            if (compareAndSetState(c, nextc))
                // Releasing the read lock has no effect on readers,
                // but it may allow waiting writers to proceed if
                // both read and write locks are now free.
                return nextc == 0;
        }
    }    
}
```