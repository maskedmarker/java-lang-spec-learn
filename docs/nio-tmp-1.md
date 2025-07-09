

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

```text
在Java NIO中，ServerSocketChannel 的 register 和 bind 方法的调用顺序是有明确要求的：

正确的顺序：先 bind 后 register
bind(SocketAddress local)：将通道绑定到指定的本地地址（端口）。
register(Selector sel, int ops)：将通道注册到选择器，并指定感兴趣的事件（如 OP_ACCEPT）。

如果先 register 再 bind 会发生什么？
会抛出 IllegalBlockingModeException
register 方法要求通道必须处于非阻塞模式（通过 configureBlocking(false) 设置）。但即使你设置了非阻塞模式，如果先 register 再 bind，仍然可能因为通道未绑定而无法正常监听连接请求，导致逻辑错误。

底层依赖
register 需要通道已经绑定到一个本地地址，因为选择器（Selector）需要知道具体的网络地址来监听事件。如果未绑定，选择器无法正确工作。

总结
必须先调用 bind 再调用 register。反之会导致异常或逻辑错误。这是由NIO的设计和底层系统调用（如操作系统的socket和bind）的语义决定的。


上面的结论是不正确的.netty先register再bind.我自己实验也没问题.
```

```text
SelectionKey.isReadable
当socket的input buffer中有数据时,该方法返回true;当该socket被对方关闭时,该方法也返回true

SocketChannel
A socket channel is created by invoking one of the open methods of this class. It is not possible to create a channel for an arbitrary, pre-existing socket.
```

```text
在StackOverflow看到一个描述,说是jdk的nio代码设计的太差了.selector/socketChannel/serverSocketChannel都不是线程安全的(或者是bug).
尽量单线程操作nio的类,这样可以规避潜在的风险.
```