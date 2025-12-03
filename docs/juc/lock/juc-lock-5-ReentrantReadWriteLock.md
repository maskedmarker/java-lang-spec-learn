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

ReadLock

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

WriteLock
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

辅助类
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


Sync
```text
abstract static class Sync extends AbstractQueuedSynchronizer {

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }
}
```