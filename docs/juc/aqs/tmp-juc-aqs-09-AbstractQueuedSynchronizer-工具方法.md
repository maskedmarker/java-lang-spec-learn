# juc-AQS-方法注释-AbstractQueuedSynchronizer


## 工具方法

### isOnSyncQueue

入参的node初始位于条件队列中,判断此时该节点是否已经转移到同步队列中了.

Returns true if a node, always one that was initially placed on a condition queue, is now waiting to reacquire on sync queue.


isOnSyncQueue等价写法,逻辑更加明了
```text
final boolean isOnSyncQueue(Node node) {
    // 条件队列中的节点都是(.waitStatus == Node.CONDITION 且.prev == null) | <利用条件队列中节点的invariant来判断是否在条件队列中>
    if (!(node.waitStatus == Node.CONDITION && node.prev == null))
        return false;
    
    // 同步队列中的非尾节点的.next都是非null,此时node是中间节点    | <利用同步队列中间节点的invariant来判断是否在条件队列中>
    if (node.next != null)
        return true;
    
    // 其实完全可以直接遍历同步队列(即prev队列)来检查,前面的2个if都是优化写法
    return findNodeFromTail(node);
}
```

### transferAfterCancelledWait


Transfers node, if necessary, to sync queue after a cancelled wait. (将等待节点转移到同步队列中)
Returns true if thread was cancelled before being signalled.(如果转移)

```text
final boolean transferAfterCancelledWait(Node node) {
    if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
        enq(node);
        return true;
    }
    /*
     * If we lost out to a signal(), then we can't proceed until it finishes its enq().  Cancelling during an incomplete transfer is both rare and transient, so just spin.
     */
    while (!isOnSyncQueue(node))
        Thread.yield();
    return false;
}
```


