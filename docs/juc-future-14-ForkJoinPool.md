# juc-ForkJoinPool

## ForkJoinPool

```text

final boolean tryExternalUnpush(ForkJoinTask<?> task) {
    WorkQueue[] ws; WorkQueue w; ForkJoinTask<?>[] a; int m, s;
    int r = ThreadLocalRandom.getProbe();
    if ((ws = workQueues) != null && (m = ws.length - 1) >= 0 &&
        (w = ws[m & r & SQMASK]) != null &&
        (a = w.array) != null && (s = w.top) != w.base) {
        long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
        if (U.compareAndSwapInt(w, QLOCK, 0, 1)) {
            if (w.top == s && w.array == a &&
                U.getObject(a, j) == task &&
                U.compareAndSwapObject(a, j, task, null)) {
                U.putOrderedInt(w, QTOP, s - 1);
                U.putOrderedInt(w, QLOCK, 0);
                return true;
            }
            U.compareAndSwapInt(w, QLOCK, 1, 0);
        }
    }
    return false;
}
```