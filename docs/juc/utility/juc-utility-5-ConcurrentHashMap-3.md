# juc-ConcurrentHashMap

## 红黑树相关



### TreeBins

TreeNodes used at the heads of bins. 
TreeBins do not hold user keys or values, but instead point to list of TreeNodes and their root. They also maintain a parasitic read-write lock forcing writers (who hold bin lock) to wait for readers (who do not) to complete before tree restructuring operations.

### treeifyBin

```text
private final void treeifyBin(Node<K,V>[] tab, int index) {
    Node<K,V> b; int n, sc;
    if (tab != null) {
        if ((n = tab.length) < MIN_TREEIFY_CAPACITY)                                                    // 数量太小时,对tab扩容,而非对tab[index]哈希桶树化
            tryPresize(n << 1);
        
        else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {                                      // b.hash >= 0是为了防止正在扩容
            synchronized (b) {                                                                           // 对哈希桶tab[index]加锁💯
                if (tabAt(tab, index) == b) {
                    TreeNode<K,V> hd = null, tl = null;
                    for (Node<K,V> e = b; e != null; e = e.next) {
                        TreeNode<K,V> p = new TreeNode<K,V>(e.hash, e.key, e.val, null, null);
                        if ((p.prev = tl) == null)
                            hd = p;
                        else
                            tl.next = p;
                        tl = p;
                    }
                    setTabAt(tab, index, new TreeBin<K,V>(hd));                                         // 此时的树是一棵退化树,型似链表.TreeBin构造函数会将退化树转换为红黑平衡二叉树
                }
            }
        }
    }
}
```

### TreeBin

```text
TreeBin(TreeNode<K,V> b) {                                                                        // 基于初始的退化树,按照红黑平衡二叉树的要求重构
    super(TREEBIN, null, null, null);
    this.first = b;
    TreeNode<K,V> r = null;
    for (TreeNode<K,V> x = b, next; x != null; x = next) {
        next = (TreeNode<K,V>)x.next;
        x.left = x.right = null;
        if (r == null) {
            x.parent = null;
            x.red = false;
            r = x;
        }
        else {
            K k = x.key;
            int h = x.hash;
            Class<?> kc = null;
            for (TreeNode<K,V> p = r;;) {
                int dir, ph;
                K pk = p.key;
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0)
                    dir = tieBreakOrder(k, pk);
                    TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    x.parent = xp;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    r = balanceInsertion(r, x);
                    break;
                }
            }
        }
    }
    this.root = r;
    assert checkInvariants(root);
}
```

### putTreeVal

```text
final TreeNode<K,V> putTreeVal(int h, K k, V v) {
    Class<?> kc = null;
    boolean searched = false;
    for (TreeNode<K,V> p = root;;) {
        int dir, ph; K pk;
        if (p == null) {
            first = root = new TreeNode<K,V>(h, k, v, null, null);
            break;
        }
        else if ((ph = p.hash) > h)
            dir = -1;
        else if (ph < h)
            dir = 1;
        else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
            return p;
        else if ((kc == null &&
                  (kc = comparableClassFor(k)) == null) ||
                 (dir = compareComparables(kc, k, pk)) == 0) {
            if (!searched) {
                TreeNode<K,V> q, ch;
                searched = true;
                if (((ch = p.left) != null &&
                     (q = ch.findTreeNode(h, k, kc)) != null) ||
                    ((ch = p.right) != null &&
                     (q = ch.findTreeNode(h, k, kc)) != null))
                    return q;
            }
            dir = tieBreakOrder(k, pk);
        }

        TreeNode<K,V> xp = p;
        if ((p = (dir <= 0) ? p.left : p.right) == null) {
            TreeNode<K,V> x, f = first;
            first = x = new TreeNode<K,V>(h, k, v, f, xp);
            if (f != null)
                f.prev = x;
            if (dir <= 0)
                xp.left = x;
            else
                xp.right = x;
            if (!xp.red)
                x.red = true;
            else {
                lockRoot();
                try {
                    root = balanceInsertion(root, x);
                } finally {
                    unlockRoot();
                }
            }
            break;
        }
    }
    assert checkInvariants(root);
    return null;
}
```



### untreeify

Returns a list on non-TreeNodes replacing those in given list.

```text
static <K,V> Node<K,V> untreeify(Node<K,V> b) {
    Node<K,V> hd = null, tl = null;
    for (Node<K,V> q = b; q != null; q = q.next) {
        Node<K,V> p = new Node<K,V>(q.hash, q.key, q.val, null);
        if (tl == null)
            hd = p;
        else
            tl.next = p;
        tl = p;
    }
    return hd;
}
```



### find 

java.util.concurrent.ConcurrentHashMap.Node.find

💯💯如果当前节点是TreeBin,find方法利用红黑树的链表视图进行遍历查找
```text
Node<K,V> find(int h, Object k) {
    Node<K,V> e = this;
    if (k != null) {
        do {
            K ek;
            if (e.hash == h && ((ek = e.key) == k || (ek != null && k.equals(ek))))
                return e;
        } while ((e = e.next) != null);
    }
    return null;
}
```

java.util.concurrent.ConcurrentHashMap.ForwardingNode.find


```text
Node<K,V> find(int h, Object k) {
    // loop to avoid arbitrarily deep recursion on forwarding nodes
    outer: for (Node<K,V>[] tab = nextTable;;) {
        Node<K,V> e; int n;
        if (k == null || tab == null || (n = tab.length) == 0 || (e = tabAt(tab, (n - 1) & h)) == null)               // 给定hashCode就能找到对应的哈希桶
            return null;
        
        for (;;) {
            int eh; K ek;
            if ((eh = e.hash) == h && ((ek = e.key) == k || (ek != null && k.equals(ek))))                            // 哈希桶头节点刚好命中  
                return e;
            
            if (eh < 0) {
                if (e instanceof ForwardingNode) {
                    tab = ((ForwardingNode<K,V>)e).nextTable;
                    continue outer;                                                                                   // 在新的哈希表中查找
                }
                else
                    return e.find(h, k);
            }
            if ((e = e.next) == null)
                return null;
        }
    }
}
```

