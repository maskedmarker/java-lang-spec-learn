# juc-CopyOnWriteArraySet

```text
public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements java.io.Serializable {

    // 底层使用CopyOnWriteArrayList就可以支持CopyOnWriteArraySet所需的操作要求
    private final CopyOnWriteArrayList<E> al;
}
```