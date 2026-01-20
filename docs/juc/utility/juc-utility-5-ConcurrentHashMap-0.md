# juc-ConcurrentHashMap


```text
普通数组中的每个位置被成为槽位slot;哈希表数组中的每个位置被成为哈希桶bucket/bin(通常是一个链表).
slot凸显的是只能容纳一个值,bucket/bin凸显的是可以容纳多个值.

初始情况下,ConcurrentHashMap的哈希桶是一个链表,当数据量比较大的时候,为了降低操作时间复杂度,链表将变化为红黑树.
```

```text
The table is lazily initialized to a power-of-two size upon the first insertion.  
Each bin in the table normally contains a list of Nodes (most often, the list has only zero or one Node).
Table accesses require volatile/atomic reads, writes, and CASes. Because there is no other way to arrange this without adding further indirections, we use intrinsics (sun.misc.Unsafe) operations.


We use the top (sign) bit of Node hash fields for control purposes -- it is available anyway because of addressing constraints.  Nodes with negative hash fields are specially handled or ignored in map methods.

Insertion (via put or its variants) of the first node in an empty bin is performed by just CASing it to the bin.  This is by far the most common case for put operations under most key/hash distributions.  
Other update operations (insert, delete, and replace) require locks.  
We do not want to waste the space required to associate a distinct lock object with each bin, so instead use the first node of a bin list itself as a lock. 
Locking support for these locks relies on builtin "synchronized" monitors.

Using the first node of a list as a lock does not by itself suffice though: 
When a node is locked, any update must first validate that it is still the first node after locking it, and retry if not. 
Because new nodes are always appended to lists, once a node is first in a bin, it remains first until deleted or the bin becomes invalidated (upon resizing).




The table is resized when occupancy exceeds a percentage threshold (nominally, 0.75, but see below). 
Any thread noticing an overfull bin may assist in resizing after the initiating thread allocates and sets up the replacement array. (只有一个发起扩容的线程,其他线程是协助扩容)
However, rather than stalling, these other threads may proceed with insertions etc.  
The use of TreeBins shields us from the worst case effects of overfilling while resizes are in progress.  
Resizing proceeds by transferring bins, one by one, from the table to the next table. 
However, threads claim small blocks of indices to transfer (via field transferIndex) before doing so, reducing contention.  
A generation stamp in field sizeCtl ensures that resizings do not overlap. 
Because we are using power-of-two expansion, the elements from each bin must either stay at same index, or move with a power of two offset. 
We eliminate unnecessary node creation by catching cases where old nodes can be reused because their next fields won't change.  
On average, only about one-sixth of them need cloning when a table doubles. 
The nodes they replace will be garbage collectable as soon as they are no longer referenced by any reader thread that may be in the midst of concurrently traversing table.  
Upon transfer, the old table bin contains only a special forwarding node (with hash field "MOVED") that contains the next table as its key. On encountering a forwarding node, access and update operations restart, using the new table.

Each bin transfer requires its bin lock, which can stall waiting for locks while resizing. 
However, because other threads can join in and help resize rather than contend for locks, average aggregate waits become shorter as resizing progresses.  
The transfer operation must also ensure that all accessible bins in both the old and new table are usable by any traversal.  
This is arranged in part by proceeding from the last bin (table.length - 1) up towards the first.  
Upon seeing a forwarding node, traversals (see class Traverser) arrange to move to the new table without revisiting nodes.  
To ensure that no intervening nodes are skipped even when moved out of order, a stack (see class TableStack) is created on first encounter of a forwarding node during a traversal, to maintain its place if later processing the current table. 
The need for these save/restore mechanics is relatively rare, but when one forwarding node is encountered, typically many more will be. 
So Traversers use a simple caching scheme to avoid creating so many new TableStack nodes. (Thanks to Peter Levart for suggesting use of a stack here.)
```



## TreeBin

TreeBin的javadoc描述以及对此段文字的解释

```text
TreeNodes used at the heads of bins. 
TreeBins do not hold user keys or values, but instead point to list of TreeNodes and their root. 
They also maintain a parasitic read-write lock forcing writers (who hold bin lock) to wait for readers (who do not) to complete before tree restructuring operations.
```

```text
1. “TreeNodes used at the heads of bins.”

In ConcurrentHashMap: A bin (bucket) is normally a linked list of Node<K,V>.When a bin is treeified, the bin head becomes a TreeBin,The actual key/value entries become TreeNode objects.

So the real hierarchy is:
table[i]
  └── TreeBin   (bin head, control object)
        ├── root  -> TreeNode (red-black tree root)
        └── first -> TreeNode (linked-list order)
        
Important correction: TreeNodes are NOT the bin head;TreeBin is the bin head;TreeNodes are the actual entry nodes stored inside the bin.




2. “TreeBins do not hold user keys or values”

TreeBin作为一个管理并发的桥接节点,其本身并不携带key-value数据.💯💯💯
TreeBins do not hold user keys or values.Instead, it is a container/controller object whose responsibilities are:
1. Point to the red-black tree (root)
2. Maintain a linked-list view (first)
3. Coordinate concurrent access during tree operations.



3. “but instead point to list of TreeNodes and their root”

This describes the dual representation inside TreeBin.A treeified bin maintains two views of the same data:
1. Red-black tree view (for lookup performance. Used for get/put/remove. Guarantees O(log n) lookup)
2. Linked-list view (for iteration and untreeify. Used for Iteration/Splitting during resize/Untreeify back to list if size shrinks.)



4. “They also maintain a parasitic read-write lock”

The lock is called parasitic because:💯💯💯
It is not a normal ReentrantReadWriteLock
It does not block readers
It piggybacks on bin-level synchronization(仅哈希桶级别的同步)

In other words:
Readers proceed mostly lock-free
Writers coordinate using a lightweight state machine



5. “forcing writers (who hold bin lock) to wait for readers (who do not)”

Writers are operations that modify tree structure, such as: Tree rotations/Rebalancing/Insertion into tree/Removal from tree
Readers are operations like: get/containsKey

Writers must:
Hold the bin lock (via synchronized (bin))
Must see a stable tree


Readers must:
Do NOT take the bin lock
Traverse the tree optimistically
Increment READER count in lockState


Tree restructuring (rotations, recoloring) is not safe if: A reader is concurrently traversing pointers
Therefore:
    Writer sets WRITER bit
    If READER bits are present: Writer waits
    New readers are blocked once writer intent is visible
This guarantees: Readers see either the old tree or the new tree, never a torn structure
```


```text
static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;   // 哈希值还兼容其他功能. -1表示正在resizing  -2表示该节点是TreeBin
        final K key;
        volatile V val;
        volatile Node<K,V> next;
}


// TreeNode也可以用.next来构建一个链表
static final class TreeNode<K,V> extends Node<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;                              // 通过prev形成双向链表,这样在remove操作时更方便更新next值
        boolean red;
}



// TreeBin是管理节点,Node中的key/value都是null,且hash == TREEBIN (-2)
static final class TreeBin<K,V> extends Node<K,V> {
        TreeNode<K,V> root;               // 红黑树视图
        volatile TreeNode<K,V> first;     // 双向链表视图
        
        volatile Thread waiter;
        volatile int lockState;
        
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock
}



// 库容中的数据迁移是按照哈希桶的维度来进行的,当完成一个旧哈希桶数据的迁移,就在原数组中设置一个类似于重定向的元素,
// 扩容时发生的读操作,要么读取到ConcurrentHashMap.table的数据,要么通过ForwardingNode读取到ConcurrentHashMap.nextTable的数据 (通过Node.hash值>0或<0来选择)
static final class ForwardingNode<K,V> extends Node<K,V> {
        
        // nextTable来自ConcurrentHashMap.nextTable, 当在扩容中时,ConcurrentHashMap.nextTable非null;扩容结束后ConcurrentHashMap.nextTable恢复为null
        final Node<K,V>[] nextTable;
        
        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }
}

哈希桶的头节点可以是,
普通链表时,头节点就是Node
红黑树时,头节点就是TreeBin
发生扩容时,头节点就是ForwardingNode
```


