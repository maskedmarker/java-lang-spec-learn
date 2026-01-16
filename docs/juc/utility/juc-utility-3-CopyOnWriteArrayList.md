# juc-CopyOnWriteArrayList

```text
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

    final transient ReentrantLock lock = new ReentrantLock();
    private transient volatile Object[] array;
    
    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }
    
    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }
}
```


```text
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    
    // 加锁
    lock.lock();
    try {
        Object[] elements = getArray();                                // 获取数组快照
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1);      // 复制快照内容
        newElements[len] = e;
        setArray(newElements);                                        // 基于复制品修改,然后将修改后的复制品放回去
        return true;
    } finally {
        lock.unlock();
    }
}


public E remove(int index) {
    final ReentrantLock lock = this.lock;
    
    lock.lock();
    try {
        Object[] elements = getArray();                                    // 获取数组快照
        int len = elements.length;
        E oldValue = get(elements, index);
        int numMoved = len - index - 1;
        if (numMoved == 0)
            setArray(Arrays.copyOf(elements, len - 1));
        else {
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index, numMoved);
            setArray(newElements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}    


public int indexOf(E e, int index) {
    Object[] elements = getArray();                         // 获取数组快照
    return indexOf(e, elements, index, elements.length);
}

public ListIterator<E> listIterator() {
    return new COWIterator<E>(getArray(), 0);               // 基于数组快照遍历
}

public boolean addIfAbsent(E e) {
    Object[] snapshot = getArray();
    return indexOf(e, snapshot, 0, snapshot.length) >= 0 ? false : addIfAbsent(e, snapshot);
}

private boolean addIfAbsent(E e, Object[] snapshot) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] current = getArray();
        int len = current.length;
        if (snapshot != current) {
            // Optimize for lost race to another addXXX operation
            int common = Math.min(snapshot.length, len);
            for (int i = 0; i < common; i++)
                if (current[i] != snapshot[i] && eq(e, current[i]))
                    return false;
            if (indexOf(e, current, common, len) >= 0)
                    return false;
        }
        Object[] newElements = Arrays.copyOf(current, len + 1);
        newElements[len] = e;
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```