# InterruptibleChannel


## Thread.interrupt

```text
// The object in which this thread is blocked in an interruptible I/O operation, if any.  The blocker's interrupt method should be invoked after setting this thread's interrupt status.
private volatile Interruptible blocker;   // Interruptible是执行可中断I/O操作的callback,具体InterruptibleChannel子类自己来实现
private final Object blockerLock = new Object();

public void interrupt() {
    if (this != Thread.currentThread()) {
        checkAccess();

        // thread may be blocked in an I/O operation
        synchronized (blockerLock) {
            Interruptible b = blocker;    // 通常是SocketChannel/ServerSocketChannel
            if (b != null) {
                interrupt0();             // set interrupt status
                b.interrupt(this);        // 先设置线程的中断标识,再执行Interruptible的interrupt方法,interrupt方法的入参是被I/O操作阻塞的线程(也是被中断的线程,而非发起中断的线程)
                return;
            }
        }
    }

    // 线程中断自己时,并不会中断当前阻塞的I/O操作
    interrupt0();                            // set interrupt status
}
```

### java.nio.channels.spi.AbstractInterruptibleChannel


#### begin/end

```text
// 实现可中断I/O操作的机制
private Interruptible interruptor;        // 发起中断的线程
private volatile Thread interrupted;      // 被中断的线程

protected final void begin() {
    if (interruptor == null) {
        interruptor = new Interruptible() {
                public void interrupt(Thread target) {
                    synchronized (closeLock) {
                        if (closed)
                            return;
                        closed = true;
                        interrupted = target; // interrupt方法的入参就是被中断的线程
                        try {
                            AbstractInterruptibleChannel.this.implCloseChannel();   // 当线程被可中断I/O操作阻塞时,中断线程会引发可中断I/O操作的目标I/O对象引发close
                        } catch (IOException x) { }
                    }
                }};
    }
    blockedOn(interruptor);                     // 设置Thread.blocker属性 InterruptibleChannel子类自己来实现可中断I/O操作的callback逻辑
    
    
    Thread me = Thread.currentThread();
    if (me.isInterrupted())
        interruptor.interrupt(me);             // 如果执行connect操作的当前线程已经被中断了,立即执行Interruptible.interrupt方法
}

protected final void end(boolean completed) throws AsynchronousCloseException {
    blockedOn(null);                                                        // I/O操作结束时,清空Thread.blocker属性
    Thread interrupted = this.interrupted;
    if (interrupted != null && interrupted == Thread.currentThread()) {     
        this.interrupted = null;                                            // 清空interrupted属性
        throw new ClosedByInterruptException();                             // interrupted == Thread.currentThread()时,标识开始I/O操作时当前线程已经被中断了且触发了同步关闭InterruptibleChannel,所以抛出的是同步关闭异常
    }
    if (!completed && closed)
        throw new AsynchronousCloseException();                             // InterruptibleChannel是被异步关闭的,所以抛出异步关闭异常
}
```

#### beginXXX/endXXX

```text

private void beginConnect(boolean blocking, InetSocketAddress isa) throws IOException {
    // 非阻塞模式没有阻塞过程,无法被中断,不用管I/O操作的可中断机制
    if (blocking) {
        // set hook for Thread.interrupt
        begin();
    }
    synchronized (stateLock) {
        ensureOpen();
        int state = this.state;
        // ...
    }
}


private void endConnect(boolean blocking, boolean completed) throws IOException {
    endRead(blocking, completed);

    if (completed) {
        synchronized (stateLock) {
            if (state == ST_CONNECTIONPENDING) {
                localAddress = Net.localAddress(fd);
                state = ST_CONNECTED;
            }
        }
    }
}

private void beginRead(boolean blocking) throws ClosedChannelException {
    if (blocking) {
        // set hook for Thread.interrupt
        begin();

        synchronized (stateLock) {
            ensureOpen();
            // record thread so it can be signalled if needed
            readerThread = NativeThread.current();
        }
    }
}
    
private void endRead(boolean blocking, boolean completed) throws AsynchronousCloseException {
    if (blocking) {
        synchronized (stateLock) {
            readerThread = 0;
            // notify any thread waiting in implCloseSelectableChannel
            if (state == ST_CLOSING) {
                stateLock.notifyAll();
            }
        }
        // remove hook for Thread.interrupt
        end(completed);
    }
}


private void beginWrite(boolean blocking) throws ClosedChannelException {
    if (blocking) {
        // set hook for Thread.interrupt
        begin();

        synchronized (stateLock) {
            ensureOpen();
            if (isOutputClosed)
                throw new ClosedChannelException();
            // record thread so it can be signalled if needed
            writerThread = NativeThread.current();
        }
    }
}

private void endWrite(boolean blocking, boolean completed) throws AsynchronousCloseException {
    if (blocking) {
        synchronized (stateLock) {
            writerThread = 0;
            // notify any thread waiting in implCloseSelectableChannel
            if (state == ST_CLOSING) {
                stateLock.notifyAll();
            }
        }
        // remove hook for Thread.interrupt
        end(completed);
    }
}
```



#### connect

```text
public boolean connect(SocketAddress sa) throws IOException {
    InetSocketAddress isa = Net.checkAddress(sa);
    InetAddress ia = isa.getAddress();

    try {
        readLock.lock();
        try {
            writeLock.lock();
            try {
                int n = 0;
                boolean blocking = isBlocking();
                try {
                    beginConnect(blocking, isa);                               // 设置interruptor
                    do {
                        n = Net.connect(fd, ia, isa.getPort());
                    } while (n == IOStatus.INTERRUPTED && isOpen());
                } finally {
                    endConnect(blocking, (n > 0));                             // 清理interruptor
                }
                assert IOStatus.check(n);
                return n > 0;
            } finally {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    } catch (IOException ioe) {
        // ...
    }
}
```

#### read

```text
public int read(ByteBuffer buf) throws IOException {
    Objects.requireNonNull(buf);

    readLock.lock();
    try {
        ensureOpenAndConnected();
        boolean blocking = isBlocking();
        int n = 0;
        try {
            beginRead(blocking);                                    // 设置interruptor

            // check if input is shutdown
            if (isInputClosed)
                return IOStatus.EOF;

            if (blocking) {
                do {
                    n = IOUtil.read(fd, buf, -1, nd);
                } while (n == IOStatus.INTERRUPTED && isOpen());
            } else {
                n = IOUtil.read(fd, buf, -1, nd);
            }
        } finally {
            endRead(blocking, n > 0);                                  // 清理interruptor
            if (n <= 0 && isInputClosed)
                return IOStatus.EOF;
        }
        return IOStatus.normalize(n);
    } finally {
        readLock.unlock();
    }
}
```

#### write

```text
public int write(ByteBuffer buf) throws IOException {
    Objects.requireNonNull(buf);
    writeLock.lock();
    try {
        ensureOpenAndConnected();
        boolean blocking = isBlocking();
        int n = 0;
        try {
            beginWrite(blocking);                                      // 设置interruptor
            if (blocking) {
                do {
                    n = IOUtil.write(fd, buf, -1, nd);
                } while (n == IOStatus.INTERRUPTED && isOpen());
            } else {
                n = IOUtil.write(fd, buf, -1, nd);
            }
        } finally {
            endWrite(blocking, n > 0);                                 // 清理interruptor
            if (n <= 0 && isOutputClosed)
                throw new AsynchronousCloseException();
        }
        return IOStatus.normalize(n);
    } finally {
        writeLock.unlock();
    }
}
```

#### bind

bind操作无法中断(因为bind操作不发生阻塞,而connect/read/write会发生阻塞)

```text
public SocketChannel bind(SocketAddress local) throws IOException {
    readLock.lock();
    try {
        writeLock.lock();
        try {
            synchronized (stateLock) {
                ensureOpen();
                // ...
                NetHooks.beforeTcpBind(fd, isa.getAddress(), isa.getPort());
                Net.bind(fd, isa.getAddress(), isa.getPort());
                localAddress = Net.localAddress(fd);
            }
        } finally {
            writeLock.unlock();
        }
    } finally {
        readLock.unlock();
    }
    return this;
}
```

#### implCloseSelectableChannel

```text
SocketChannelImpl的状态

// State, increases monotonically(单调地,单向地)
private static final int ST_UNCONNECTED = 0;
private static final int ST_CONNECTIONPENDING = 1;
private static final int ST_CONNECTED = 2;
private static final int ST_CLOSING = 3;
private static final int ST_KILLPENDING = 4;
private static final int ST_KILLED = 5;

private volatile int state;  // need stateLock to change
```

```text
protected void implCloseSelectableChannel() throws IOException {
    assert !isOpen();

    boolean blocking;
    boolean connected;
    boolean interrupted = false;

    // set state to ST_CLOSING
    synchronized (stateLock) {
        assert state < ST_CLOSING;
        blocking = isBlocking();
        connected = (state == ST_CONNECTED);
        state = ST_CLOSING;                                                // 💯💯💯先设置逻辑关闭的状态
    }
    // 💯当前channel已经是逻辑上的关闭状态,如下要执行真正的关闭动作
    
    
    // wait for any outstanding I/O operations to complete
    if (blocking) {
        synchronized (stateLock) {
            assert state == ST_CLOSING;
            long reader = readerThread;
            long writer = writerThread;
            if (reader != 0 || writer != 0) {
                nd.preClose(fd);                                               // 💯💯💯预先关闭文件描述符,使阻塞IO立即返回
                connected = false; // fd is no longer connected socket

                if (reader != 0)
                    NativeThread.signal(reader);
                if (writer != 0)
                    NativeThread.signal(writer);

                // wait for blocking I/O operations to end 等待I/O操作完成
                while (readerThread != 0 || writerThread != 0) {
                    try {
                        stateLock.wait();                                      // endRead/endWrite都会在I/O操作完成后调用stateLock.notifyAll()
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
        }
    } else {
        // 优雅的等待方式：非阻塞模式下,通过获取释放读写锁来确保正在进行的I/O操作都已结束(非阻塞模式下,I/O操作无阻塞,很快结束)
        readLock.lock();
        try {
            writeLock.lock();
            writeLock.unlock();
        } finally {
            readLock.unlock();
        }
    }

    // set state to ST_KILLPENDING
    synchronized (stateLock) {
        assert state == ST_CLOSING;
        // if connected and the channel is registered with a Selector then shutdown the output if possible so that the peer reads EOF. 
        // If SO_LINGER is enabled and set to a non-zero value then it needs to be disabled so that the Selector does not wait when it closes the socket.
        if (connected && isRegistered()) {
            try {
                SocketOption<Integer> opt = StandardSocketOptions.SO_LINGER;
                int interval = (int) Net.getSocketOption(fd, Net.UNSPEC, opt);
                if (interval != 0) {
                    if (interval > 0) {
                        // disable SO_LINGER
                        Net.setSocketOption(fd, Net.UNSPEC, opt, -1);
                    }
                    Net.shutdown(fd, Net.SHUT_WR);
                }
            } catch (IOException ignore) { }
        }
        state = ST_KILLPENDING;                             // 💯 设置逻辑kill,再执行kill动作(减少持有锁的时间)
    }

    // close socket if not registered with Selector
    if (!isRegistered())
        kill();

    // restore interrupt status
    if (interrupted)
        Thread.currentThread().interrupt();
}
```