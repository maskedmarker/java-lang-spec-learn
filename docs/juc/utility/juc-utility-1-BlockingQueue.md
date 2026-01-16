# juc-BlockingQueue


## BlockingQueue
```text
A Queue that additionally supports operations that wait for the queue to become non-empty when retrieving an element, and wait for space to become available in the queue when storing an element.
除了支持数据结构队列Queue常用操作外,而且还支持等待:
    如果移除数据时,此时队列没有数据了,执行移除操作的线程会等待直到队列有数据为止.
    如果存储数据时,此时队列没有空间了,执行存储操作的线程会等待直到队列有空间为止.
```

```text
BlockingQueue的方法可以分为4类:
1. 操作无法立即完成就抛出异常,提示操作失败.
2. 操作无法立即完成时,则返回false/null提示操作失败.
3. 操作无法立即完成时一直阻塞等待,直到操作成功.
4. 操作无法立即完成时阻塞等待一段时间,超过指定时间后还不能完成操作,则返回false/null提示操作失败.

如上的等待都支持中断,中断可以结束阻塞等待.

+----------------------------+----------------------------------+---------------------------------+-----------------------------------------+--------------------------------------+
|          操作类型            |         操作无法立即完成时抛出异常   |    操作无法立即完成时返回false/null |    操作无法立即完成时等待条件满足             |          操作无法立即完成时等待一段时间    |
+----------------------------+----------------------------------+---------------------------------+-----------------------------------------+--------------------------------------+
|        Insert              |            add(e)                |              offer(e)           |            put(e)                       |       offer(e, time, unit)           |
+----------------------------+----------------------------------+---------------------------------+-----------------------------------------+--------------------------------------+
|        Remove              |           remove()               |              poll()             |             take()                      |        poll(time, unit)              |
+----------------------------+----------------------------------+---------------------------------+-----------------------------------------+--------------------------------------+
|       Examine              |           element()              |              peek()             |            无                           |                  无                   |
+----------------------------+----------------------------------+---------------------------------+-----------------------------------------+--------------------------------------+


add/remove都可以用offer/poll来实现,只需要判断offer/poll的返回值为false/null后抛出异常即可.
所以BlockingQueue真正需要实现的方法是: offer/poll, put/take, offer(timeout)/poll(timeout)
```



## ArrayBlockingQueue

不支持动态扩容

```text
public class ArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    /** The queued items */
    final Object[] items;

    /** items index for next take, poll, peek or remove */
    int takeIndex;

    /** items index for next put, offer, or add */
    int putIndex;

    /** Number of elements in the queue */
    int count;
    
    final ReentrantLock lock;

    /** Condition for waiting takes */
    private final Condition notEmpty;

    /** Condition for waiting puts */
    private final Condition notFull;
    
    
    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0) throw new IllegalArgumentException();
            
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull =  lock.newCondition();
    }    
}
```


### 阻塞无期限
```text
public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();                 // 加锁防止并发操作(支持中断)
    try {
        while (count == items.length)        // 等待队列有空间保存数据(中断结束等待并抛出异常)
            notFull.await();
        
        enqueue(e);
    } finally {
        lock.unlock();
    }
}

public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();                 // 加锁防止并发操作(支持中断)
    try {
        while (count == 0)                    // 等待队列有空间保存数据(中断结束等待并抛出异常)
            notEmpty.await();
        
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```

### 阻塞有期限

```text
public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {

    checkNotNull(e);
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();                             // 加锁防止并发操作(支持中断)
    
    try {
        while (count == items.length) {
            if (nanos <= 0)
                return false;                             // 有限时间内等待队列有空间保存数据(中断结束等待并抛出异常)
            nanos = notFull.awaitNanos(nanos);
        }
        
        enqueue(e);
        return true;
    } finally {
        lock.unlock();
    }
}

public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();                             // 加锁防止并发操作(支持中断)
    try {
        while (count == 0) {
            if (nanos <= 0)
                return null;                              // 有限时间内等待队列有空间保存数据(中断结束等待并抛出异常)
            nanos = notEmpty.awaitNanos(nanos);
        }
        
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```

### 基础操作

基础操作都是在互斥锁的保护下进行的.

ArrayBlockingQueue.items是一个循环数组,数组的大小在ArrayBlockingQueue初始化阶段确定并不再调整大小.
putIndex/takeIndex这两个变量支持zero-wrapping,从而实现数组的循环.
count准确表示当前队列中元素的数量.

```text
private void enqueue(E x) {                 // 调用enqueue需要持有互斥锁和已经判断count<items.length
    final Object[] items = this.items;
    items[putIndex] = x;
    if (++putIndex == items.length)
        putIndex = 0;
    count++;
    
    notEmpty.signal();
}

private E dequeue() {                       // 调用dequeue需要持有互斥锁和已经判断count>0

    final Object[] items = this.items;
    E x = (E) items[takeIndex];
    items[takeIndex] = null;
    if (++takeIndex == items.length)
        takeIndex = 0;
    count--;
    if (itrs != null)
        itrs.elementDequeued();
    
    notFull.signal();                       // 与LinkedBlockingQueue相比这里的signal控制不够精细, 假如dequeue前count已经<items.length,此时signal是无用的
    
    return x;
}    
```

## LinkedBlockingQueue

容量不限.

```text
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    private final int capacity;
    private final AtomicInteger count = new AtomicInteger();
    
    transient Node<E> head;
    private transient Node<E> last;
    
    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();
    private final ReentrantLock putLock = new ReentrantLock();
    private final Condition notFull = putLock.newCondition();
    
    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }
}


```

### 阻塞无期限

put除了操作数据外还会通知notEmpty;take除了操作数据外还会通知notFull.这里就体现了并发中的协作.
而put操作数据时用到的putLock和take操作数据时用到的takeLock则体现了并发中的同步,即将同时发生的操作强制排序,有序进行.

```text
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    
    putLock.lockInterruptibly();
    try {
        while (count.get() == capacity) {
            notFull.await();
        }
        enqueue(node);
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    
    // 此时已经释放putLock
    if (c == 0)
        signalNotEmpty();
}

private void signalNotEmpty() {
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
}

put操作中,先持有putLock,释放putLock后才去持有takeLock;而take操作中,先持有takeLock,释放takeLock后才去持有putLock.不会发生死锁.

// -----------------------------------------------------------------------------------------

public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    
    takeLock.lockInterruptibly();
    try {
        while (count.get() == 0) {
            notEmpty.await();
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    
    // 此时已经释放takeLock
    if (c == capacity)
        signalNotFull();
    
    return x;
}

private void signalNotFull() {
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
        notFull.signal();
    } finally {
        putLock.unlock();
    }
}
```


### 阻塞有期限

```text
public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    
    long nanos = unit.toNanos(timeout);
    int c = -1;
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    
    putLock.lockInterruptibly();
    try {
        while (count.get() == capacity) {
            if (nanos <= 0)
                return false;                             // 有限阻塞
            nanos = notFull.awaitNanos(nanos);
        }
        enqueue(new Node<E>(e));
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    
    if (c == 0)
        signalNotEmpty();
    
    return true;
}


public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    E x = null;
    int c = -1;
    long nanos = unit.toNanos(timeout);
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    
    takeLock.lockInterruptibly();
    try {
        while (count.get() == 0) {
            if (nanos <= 0)
                return null;                                  // 有限阻塞
            nanos = notEmpty.awaitNanos(nanos);
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    
    if (c == capacity)
        signalNotFull();
    
    return x;
}
```

### 基础操作

```text
private void enqueue(Node<E> node) {
    // assert putLock.isHeldByCurrentThread();
    // assert last.next == null;
    
    last = last.next = node;
}

private E dequeue() {
    // assert takeLock.isHeldByCurrentThread();
    // assert head.item == null;
    
    Node<E> h = head;
    Node<E> first = h.next;
    h.next = h; // help GC
    head = first;
    E x = first.item;
    first.item = null;
    
    return x;
}
```

## PriorityBlockingQueue

容量不限.
An unbounded blocking queue that uses the same ordering rules as class PriorityQueue and supplies blocking retrieval operations.

```text
public class PriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    // 最小平衡二叉堆(可以自动扩容) a balanced binary heap. For each node n in the heap and each descendant d of n, n <= d. The element with the lowest value is in queue[0], assuming the queue is nonempty
    private transient Object[] queue;
    // 堆内元素的数据量
    private transient int size;  // The number of elements in the priority queue.
    
    private final ReentrantLock lock;
    private final Condition notEmpty;  // 因为可以自动扩容所以offer不会发生阻塞,因此不需要notFull-Condition


    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1) throw new IllegalArgumentException();
        
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.comparator = comparator;
        this.queue = new Object[initialCapacity];
    }
}
```

### 阻塞无期限

```text
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    
    lock.lockInterruptibly();                            // 加锁
    E result;
    try {
        while ( (result = dequeue()) == null)           // 等待
            notEmpty.await();
    } finally {
        lock.unlock();
    }
    
    return result;
}


// 👉 offer不会发生阻塞,因为可以扩容
public boolean offer(E e) {
    if (e == null) throw new NullPointerException();
    
    final ReentrantLock lock = this.lock;
    lock.lock();                                                   // 加锁
    
    // 扩容
    int n, cap;
    Object[] array;
    while ((n = size) >= (cap = (array = queue).length))
        tryGrow(array, cap);
    
    try {
        // 入堆操作
        Comparator<? super E> cmp = comparator;
        if (cmp == null)
            siftUpComparable(n, e, array);
        else
            siftUpUsingComparator(n, e, array, cmp);
        
        size = n + 1;
        
        notEmpty.signal();
    } finally {
        lock.unlock();
    }
    
    return true;
}
```


## DelayQueue

容量不限.
An unbounded blocking queue of Delayed elements, in which an element can only be taken when its delay has expired.
The head of the queue is that Delayed element whose delay expired furthest in the past.

```text
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {

    // 优先堆,延迟时间最近的排在前
    private final PriorityQueue<E> q = new PriorityQueue<E>();

    private final transient ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();     // 可以自动扩容,不存在notFull-condition,这里类似于notEmpty-condition
    
    // Thread designated to wait for the element at the head of the queue. 
    private Thread leader = null;
}
```

### 操作

```text
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();                                      // 为了实现take支持中断,使用Lock的支持中断方法lockInterruptibly()
    
    try {
        for (;;) {
            E first = q.peek();
            if (first == null)
                available.await();
            else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0)
                    return q.poll();
                
                first = null;           // don't retain ref while waiting
                if (leader != null)
                    available.await();
                else {
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        available.awaitNanos(delay);
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
    

// 可以动态扩容,所以不会发生阻塞
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();                               // 为了实现offer不支持中断,使用Lock的不支持中断方法lock()
    
    try {
        q.offer(e);
        if (q.peek() == e) {
            leader = null;
            available.signal();
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

## SynchronousQueue

A blocking queue in which each insert operation must wait for a corresponding remove operation by another thread, and vice versa. 
A synchronous queue does not have any internal capacity, not even a capacity of one.


```text
public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    
    abstract static class Transferer<E> {
        // Performs a put or take.
        // Params:  e – if non-null, the item to be handed to a consumer; if null, requests that transfer return an item offered by producer.
        // Returns: if non-null, the item provided or received; 
                    if null, the operation failed due to timeout or interrupt -- the caller can distinguish which of these occurred by checking Thread.interrupted.
        abstract E transfer(E e, boolean timed, long nanos);
    }
    
    // Dual stack
    static final class TransferStack<E> extends Transferer<E> {
        volatile SNode head;
    }
    
    // Dual Queue
    static final class TransferQueue<E> extends Transferer<E> {
        transient volatile QNode head;
        transient volatile QNode tail;
        transient volatile QNode cleanMe;  // Reference to a cancelled node that might not yet have been unlinked from queue because it was the last inserted node when it was cancelled.
    }
}
```

### 操作

```text
public boolean offer(E e) {
    if (e == null) throw new NullPointerException();
    
    return transferer.transfer(e, true, 0) != null;
}
    
public E take() throws InterruptedException {
    E e = transferer.transfer(null, false, 0);
    
    if (e != null)
        return e;
    
    Thread.interrupted();
    throw new InterruptedException();
}
```

#### TransferStack实现

基于Scherer–Scott Dual Stack Algorithm.
Scherer–Scott Dual Stack Algorithm is a classic non-blocking (lock-free) concurrent stack algorithm.It is best understood as an optimization of Treiber’s lock-free stack that reduces contention under high concurrency.
Treiber Stack在高并发场景下性能堪忧.
Scherer–Scott Dual Stack Maintain two logical stacks: 
    a central stack                              (Treiber stack)
    a secondary (elimination / combining) stack  (用于 push/pop 对消)
Threads dynamically choose where to operate.

核心策略
1. 先尝试 central stack
2. CAS 失败次数过多 → 进入 elimination stack
3. push和pop在elimination stack中直接配对
4. 若配对失败 → 回退 central stack

```text

```

#### TransferQueue实现