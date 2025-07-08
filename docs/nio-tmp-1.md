

```text
当你调用 SocketChannel.close() 时，是否还需要从 Selector 中手动移除对应的 SelectionKey？
不需要手动 remove，close() 会自动取消注册。


public final void close() throws IOException {
    synchronized (closeLock) {
        if (!open)
            return;
        open = false;
        implCloseChannel();
    }
}

protected final void implCloseChannel() throws IOException {
    implCloseSelectableChannel();
    synchronized (keyLock) {
        int count = (keys == null) ? 0 : keys.length;
        for (int i = 0; i < count; i++) {
            SelectionKey k = keys[i];
            if (k != null)
                k.cancel();
        }
    }
}    
```