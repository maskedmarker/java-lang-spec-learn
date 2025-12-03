# juc-AQS-ReentrantLock

AQS.state的含义:
当state==0时,锁没有被持有;
当state!=0时,锁被持有;



## 使用样例

## 源码实现

```text
public class ReentrantLock implements Lock, java.io.Serializable {
    private final Sync sync;
}
```

### Sync

```text
abstract static class Sync extends AbstractQueuedSynchronizer {

    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        
        // 为了体现非公平性,不管同步队列中是否有排队者,直接尝试cas-state来抢占锁资源
        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }

    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }
}


nonfairTryAcquire方法可以正直观地实现为:
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    
    // 不管state状态,也不管同步队列中前面的排队者,直接尝试cas-state来抢占锁资源      // 虽然这里看起来更直观,但是不如原代码更高效.原代码可以中额外的(c == 0)判断可以规避c!=0时的盲目执行cas带来的开销.💯
    if (compareAndSetState(0, acquires)) {
        setExclusiveOwnerThread(current);
        return true;
    }
    
    // 这里不能省略掉(c != 0),只是用(current == getExclusiveOwnerThread()), 防止锁状态发生异常(即c==0 && current == getExclusiveOwnerThread())
    if (c != 0 && current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    
    return false;
}
```

### FairSync
```text
static final class FairSync extends Sync {

    // Don't grant access unless recursive call or no waiters or is first.
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        
        // 当锁没有持有者,且同步队列没有更早的排队者,才可以尝试cas-state来抢占锁资源(为了FIFO)
        if (c == 0) {
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            
            // 因为是互斥锁,当前线程已经持有锁了,可以不用cas-state,只用volatile-write    
            setState(nextc);
            return true;
        }
        
        return false;
    }
}

为了实现公平,即满足FIFO
如果c==0,锁没有被持有,除非同步队列没有更早的排队者才能尝试获取锁,这样才能FIFO.
如果c!=0,锁被线程持有,除非是当前线程持有,否则就在FIFO同步队列中按顺序被唤醒尝试获取锁.
```

### NonfairSync

```text
static final class NonfairSync extends Sync {

    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }
}
```

