# juc-lock-总结

```text
tryAcquire/tryAcquireShared 只有在可能成功的条件下才尝试抢占锁(甚至多次重试).
先不考虑公平性,
对于ReentrantLock.Syn.tryAcquire,只有当state==0时,才case-state(0, 1),且由于ReentrantLock.Syn.state是二元的,一旦case-state失败不能再重试;
对于Semaphore.Syn.tryAcquireShared,只有当remaining >= 0时,才case-state(x, remaining), 且由于Semaphore.Syn.state是可以容纳多个数,一旦case-state失败只要remaining >= 0还成立,就可以多次重试;

盲目地直接cas-state(expected-old-value, new-value),只会破坏逻辑分支判断,当前jdk中的实现都是按照AQS-state的值划分逻辑分支,实现状态机的响应模式.


💯 独占模式的tryAcquire不使用重试;共享模式的tryAcquireShared使用重试.
```
