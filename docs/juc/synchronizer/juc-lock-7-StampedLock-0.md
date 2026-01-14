# juc-StampedLock

StampedLock适用于写锁持有期很短的场景(纳秒/微妙);AQS适用于锁持有期没那么短的场景.💯💯💯


```text
A capability-based lock with three modes for controlling read/write access. 
The state of a StampedLock consists of a version and mode. 
Lock acquisition methods return a stamp that represents and controls access with respect to a lock state; 
"try" versions of these methods may instead return the special value zero to represent failure to acquire access. 

A zero return from any "try" method for acquiring or converting locks does not carry any information about the state of the lock.

The scheduling policy of StampedLock does not consistently prefer readers over writers or vice versa.

StampedLocks are designed for use as internal utilities in the development of thread-safe components. 
Their use relies on knowledge of the internal properties of the data, objects, and methods they are protecting. 
They are not reentrant, so locked bodies should not call other unknown methods that may try to re-acquire locks.💯💯💯
(StampedLock不支持重入,所以使用的时候一定要注意,防止自己与自己发生死锁)

Because it supports coordinated usage across multiple lock modes, this class does not directly implement the Lock or ReadWriteLock interfaces. 
```


## 使用样例

```java
class Point {
  private double x, y;
  private final StampedLock sl = new StampedLock();

  void move(double deltaX, double deltaY) { 
    long stamp = sl.writeLock();               // an exclusively locked method   直接使用排他的写模式(可能发生阻塞等待)
    try {
      x += deltaX;
      y += deltaY;
    } finally {
      sl.unlockWrite(stamp);
    }
  }

  double distanceFromOrigin() { 
    long stamp = sl.tryOptimisticRead();    // A read-only method   只使用非排他的读模式
    double currentX = x, currentY = y;
    if (!sl.validate(stamp)) {
       stamp = sl.readLock();
       try {
         currentX = x;
         currentY = y;
       } finally {
          sl.unlockRead(stamp);
       }
    }
    return Math.sqrt(currentX * currentX + currentY * currentY);
  }

  void moveIfAtOrigin(double newX, double newY) { 
    long stamp = sl.readLock();                    // Could instead start with optimistic, not read mode   先使用非排他的读模式,后续再升级为排他的写模式
    try {
      while (x == 0.0 && y == 0.0) {
        long ws = sl.tryConvertToWriteLock(stamp);
        if (ws != 0L) {
          stamp = ws;
          x = newX;
          y = newY;
          break;
        } else {                        // upgrade 升级为排他的写模式(可能发生阻塞等待)
          sl.unlockRead(stamp);
          stamp = sl.writeLock();
        }
      }
    } finally {
      sl.unlock(stamp);
    }
  }
}
```

## cowait

cowait的核心思想：写线程“批量候选”,写锁只能一个成功,但可以让多个写线程“同时醒来竞争”. 💯💯💯
它通过批量唤醒+热竞争,将“锁释放->下一次写锁成功”的延迟压缩到极限,从而显著提升写吞吐.

```text
在传统互斥锁/AQS 写锁中,写线程的典型行为是：
W1 持锁
W2 阻塞
W3 阻塞
W4 阻塞

当 W1 释放锁时：
unpark(W2)
W2 被调度 → 抢锁 → 成功
W3、W4 继续睡眠

问题在于：
写锁本身是独占的,但“等待”阶段不必严格独占,AQS仍然让写线程严格一对一串行唤醒,造成了写锁的“无谓串行化”.

这在以下情况下吞吐非常差：
写临界区很短
写请求高度密集
CPU核心数充足
👉 锁释放的“唤醒粒度”过小,限制了并行竞争.



cowait 如何提升吞吐(关键路径分析)
① 队首线程成功后,批量唤醒 cowait
    当头节点成功获取写锁,将其cowait链上的所有写线程一次性唤醒
② 这些写线程不会立即成功,但会“就绪”
    被唤醒的写线程：全部进入RUNNABLE,它们将：观察state,自旋,尝试CAS

③ 下一次写锁释放时,竞争是“热的”
    ❌ 没有 cowait(AQS)
    unlock
      └─ unpark(W2)
            └─ OS 调度
                 └─ cache cold
                      └─ CAS
    
    ✅ 有 cowait(StampedLock)
    unlock
      └─ 多个写线程已在运行态
            └─ cache hot
                 └─ CAS竞争          
④ 减少两个关键成本
    减少park/unpark次数,减少调度延迟                
    
    
cowait不改变FIFO顺序,只是“提前唤醒”,不是“提前成功”
```

注意cowait的使用场景

```text
什么时候 cowait 提升最明显？

收益最大场景：💯💯💯
    写锁持有时间极短(μs 级)
    写请求高度密集
    多核
    非实时调度(普通Linux/Windows)

收益有限甚至负面场景：
    写锁持有时间长
    CPU 核心少
    写请求稀疏

StampedLock 的实现中已通过：HEAD_SPINS/MAX_HEAD_SPINS自适应park条件来避免这些负面情况.
```