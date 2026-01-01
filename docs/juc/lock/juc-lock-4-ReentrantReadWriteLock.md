# juc-lock-ReentrantReadWriteLock

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
    
    // --------------------------------------------- 如下是写锁操作 ------------------------------------------------------------------------
    
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
        
        // 如果c==0,仅表示没有reader/writer持有锁,(为了FIFO)还要判断队列中是否有排队的:如果已经有其他线程在排队了,当前线程尝试抢占写锁也是失败的;如果没有其他线程排队,这时才用cas-state来尝试.
        前面是否有可以直接去尝试抢占写锁; 
        if (writerShouldBlock() ||
            !compareAndSetState(c, c + acquires))
            return false;
        setExclusiveOwnerThread(current);
        return true;
    }
    
    // 写锁释放时,无并发
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
       
    // --------------------------------------------- 如下是读锁操作 ------------------------------------------------------------------------
    
    protected final int tryAcquireShared(int unused) {
        /*
         * Walkthrough:
         * 1. If write lock held by another thread, fail.
         * 2. Otherwise, this thread is eligible for lock wrt state, so ask if it should block because of queue policy. If not, try to grant by CASing state and updating count.
         *    Note that step does not check for reentrant acquires, which is postponed to full version to avoid having to check hold count in the more typical non-reentrant case.
         * 3. If step 2 fails either because thread apparently not eligible or CAS fails or count saturated, chain to version with full retry loop.
         */
        Thread current = Thread.currentThread();
        int c = getState();
        if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) // 其他线程持有写锁,获取读锁失败
            return -1; // 满足接口的要求,提供一个负数,表示操作失败.
        
        int r = sharedCount(c);
        if (!readerShouldBlock() &&                     // 为了FIFO,同步队列前面没有排队者,才能尝试抢占锁
            r < MAX_COUNT &&
            compareAndSetState(c, c + SHARED_UNIT)) {
            if (r == 0) {
                firstReader = current;                 // firstReader用来指向(所有读写锁释放后)第一个获取到读锁的线程
                firstReaderHoldCount = 1;              // 为了支持重入,firstReaderHoldCount是指firstReader线程的重入次数
            } else if (firstReader == current) {
                firstReaderHoldCount++;
            } else {
                HoldCounter rh = cachedHoldCounter;              //   cachedHoldCounter指向最后一个获取读锁的线程的重入次数(没有什么用途)    || cachedHoldCounter是普通的非volatile变量,各个线程都会维护一份副本
                if (rh == null || rh.tid != getThreadId(current))  // cachedHoldCounter中的tid用来区分这是哪个线程
                    cachedHoldCounter = rh = readHolds.get();    // readHolds指向各个线程的读锁重入次数;
                else if (rh.count == 0)
                    readHolds.set(rh);
                rh.count++;         // 每次读锁获取成功,重入次数加一
            }
            return 1; // 满足接口的要求,提供一个正数,表示操作成功.
        }
        
        // (在只有读锁的情况下)如果同步队列前面有排队者,或者同步队列前面没有排队者且自己尝试抢占锁失败了
        return fullTryAcquireShared(current);
    }

    final int fullTryAcquireShared(Thread current) {
        /*
         * This code is in part redundant with that in tryAcquireShared but is simpler overall by 
         * not complicating tryAcquireShared with interactions between retries and lazily reading hold counts.
         */
        HoldCounter rh = null;
        
        // 当只有其他线程持有读锁时,可以重试
        for (;;) {
            int c = getState();
            if (exclusiveCount(c) != 0) {
                if (getExclusiveOwnerThread() != current)
                    return -1;
                // else we hold the exclusive lock; blocking here would cause deadlock. // 允许同一线程同时持有读锁和写锁(此时其他线无法获取到读锁或写锁,本线程读写都是安全的.)
            } else if (readerShouldBlock()) {
                // Make sure we're not acquiring read lock reentrantly
                if (firstReader == current) {
                    // assert firstReaderHoldCount > 0;
                } else {
                    if (rh == null) {
                        rh = cachedHoldCounter;
                        if (rh == null ||
                            rh.tid != LockSupport.getThreadId(current)) {
                            rh = readHolds.get();
                            if (rh.count == 0)
                                readHolds.remove();
                        }
                    }
                    if (rh.count == 0)
                        return -1;
                }
            }
            
            if (sharedCount(c) == MAX_COUNT)
                throw new Error("Maximum lock count exceeded");
            
            if (compareAndSetState(c, c + SHARED_UNIT)) {
                if (sharedCount(c) == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    if (rh == null)
                        rh = cachedHoldCounter;
                    if (rh == null ||
                        rh.tid != LockSupport.getThreadId(current))
                        rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                    cachedHoldCounter = rh; // cache for release
                }
                return 1;
            }
        }
    }

    
    // 读锁释放,存在并发
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
        
        // 存在并发读锁释放,cas会因并发失败,需要cas重试
        for (;;) {
            int c = getState();
            int nextc = c - SHARED_UNIT;
            if (compareAndSetState(c, nextc))
                // 当返回true时,AQS框架会唤醒等待的线程
                // 当读锁全部释放时需要唤醒位于队头的写锁线程,如果读锁还未全部释放时就唤醒队头的写锁线程,写锁线程还会因为tryAcquire失败而重新挂起等待.
                return nextc == 0;
        }
    } 
}


为什么说读锁全部释放前,同步队列如果不为空则队头必是写锁节点?
当线程T在成为头节点并获取锁成功后,
    如果后面是连续的读锁节点(共享节点),则会级联唤醒,并依次成为队头并获得锁后出队,直到碰到一个写锁节点或者清空同步队列.(此时state中只有读锁,没有写锁)
    如果后面第一个就是写锁节点(独占节点),则不会级联唤醒,需要等待.(此时state中只有读锁,没有写锁)

当线程T在持有锁期间,
    初始状态: 此时要么同步队列清空要么队头是写锁节点;且此时state中只有读锁没有写锁
    后续:    
            如果同步队列为空,其他读锁释无需唤醒其他线程;
            如果同步队列不为空,因为读锁释放都无法也唤醒写锁线程,所以队头保持写锁节点.
            后续的读写锁线程都要往后排队.
    
当线程T释放锁时,
    初始状态: 此时要么同步队列清空要么队头是写锁节点;且此时state中只有读锁没有写锁
    后续:     
            如果其他读锁都释放了,此时可以唤醒写锁线程.(写锁线程后面释放时,唤醒下一个线程是读锁的话,会发生级联唤醒连续的读锁线程)
```