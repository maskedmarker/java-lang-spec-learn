# Selector


## WindowsSelectorImpl中为什么会使用Pipe

Linux平台的epoll函数支持主动往fd中主动写入数据从而终止select的阻塞(即Selector.wakeup()方法要达到的效果).而windows平台不支持这类功能.
jdk在windows平台的selector实现中,偷偷往selector管理的channel中添加了一个pipe.source.
当没有新数据时,调用select会导致阻塞,此时通过往pipe.sink写入数据(pipe会将sink的数据传送到pipe.source),此时selector管理的channel就有一个有数据(即pipe.source),select方法就结束阻塞并返回.
(备注: 如上当前进程自己往pipe写入数据,自己读取数据的编程技巧也被称作self-pipe trick)

```text
In sun.nio.ch.WindowsSelectorImpl, a Pipe is used to wake up and control a selector that is blocked in a native select() call. 
This design compensates for limitations of the Windows I/O multiplexing model.

Core problem: waking up a blocking select() on Windows
On all platforms, Selector must support:
    selector.wakeup()
    thread-safe register() / cancel() while another thread is blocked in select()

On Linux/Unix
epoll, kqueue, poll can be woken up via: eventfd
    
On Windows
Java NIO uses Winsock select() (not epoll-like mechanisms), Winsock select() has NO native wakeup mechanism,You cannot interrupt it.


Why Pipe works on Windows
A Pipe creates two connected endpoints:
    Sink channel → write end
    Source channel → read end

On Windows:
Pipe channels are backed by socket handles, These socket handles are selectable by Winsock select()
So Java does this: Inject a controllable socket into the select() FD set.
```


## WindowsSelectorImpl (java11)

```text
public class WindowsSelectorProvider extends SelectorProviderImpl {

    public AbstractSelector openSelector() throws IOException {
        return new WindowsSelectorImpl(this);
    }
}


WindowsSelectorImpl(SelectorProvider sp) throws IOException {
    this.provider = provider;
    keys = ConcurrentHashMap.newKeySet();
    selectedKeys = new HashSet<>();
    publicKeys = Collections.unmodifiableSet(keys);
    publicSelectedKeys = Util.ungrowableSet(selectedKeys);
    
    
    pollWrapper = new PollArrayWrapper(INIT_CAP);                                     // native array,容纳(fd, events) pairs
    wakeupPipe = Pipe.open();                                                         // 在本地开2个端口,一个client和一个server,前者作为pipe.sink,后者作为pipe.source
    wakeupSourceFd = ((SelChImpl)wakeupPipe.source()).getFDVal();

    // Disable the Nagle algorithm so that the wakeup is more immediate
    SinkChannelImpl sink = (SinkChannelImpl)wakeupPipe.sink();
    (sink.sc).socket().setTcpNoDelay(true);
    wakeupSinkFd = ((SelChImpl)sink).getFDVal();

    pollWrapper.addWakeupSocket(wakeupSourceFd, 0);                                   // 将pipe.source的fd和感兴趣的poll_in事件配置到poll操作的native array中
}
```

WindowsSelectorImpl是active-object, 其内部有独立线程来不间断执行poll操作

```text
private final class SelectThread extends Thread {
    private final int index;                     // index of this thread
    final SubSelector subSelector;
    private long lastRun = 0;                    // last run number
    private volatile boolean zombie;
    
    
    // Creates a new thread
    private SelectThread(int i) {
        super(null, null, "SelectorHelper", 0, false);
        this.index = i;
        this.subSelector = new SubSelector(i);
        this.lastRun = startLock.runsCounter;        //make sure we wait for next round of poll
    }
    
    
    public void run() {
        while (true) { // poll loop
            // wait for the start of poll. If this thread has become redundant, then exit.
            if (startLock.waitForStart(this)) {
                subSelector.freeFDSetBuffer();
                return;
            }
            // call poll()
            try {
                subSelector.poll(index);
            } catch (IOException e) {
                // Save this exception and let other threads finish.
                finishLock.setException(e);
            }
            // notify main thread, that this thread has finished, and wakeup others, if this thread is the first to finish.
            finishLock.threadFinished();
        }
    }
}
```

SubSelector类封装select调用

```text
private final class SubSelector {
        private final int pollArrayIndex; // starting index in pollArray to poll
        // These arrays will hold result of native select().
        // The first element of each array is the number of selected sockets.
        // Other elements are file descriptors of selected sockets.
        private final int[] readFds = new int [MAX_SELECTABLE_FDS + 1];
        private final int[] writeFds = new int [MAX_SELECTABLE_FDS + 1];
        private final int[] exceptFds = new int [MAX_SELECTABLE_FDS + 1];
        
        
        // poll for the main thread
        private int poll() throws IOException{ 
            return poll0(pollWrapper.pollArrayAddress,
                         Math.min(totalChannels, MAX_SELECTABLE_FDS),
                         readFds, writeFds, exceptFds, timeout, fdsBuffer);
        }

        // poll for helper threads
        private int poll(int index) throws IOException {
            
            return  poll0(pollWrapper.pollArrayAddress + (pollArrayIndex * PollArrayWrapper.SIZE_POLLFD),
                     Math.min(MAX_SELECTABLE_FDS, totalChannels - (index + 1) * MAX_SELECTABLE_FDS),
                     readFds, writeFds, exceptFds, timeout, fdsBuffer);
        }
        
        private native int poll0(long pollAddress, int numfds, int[] readFds, int[] writeFds, int[] exceptFds, long timeout, long fdsBuffer);
}
```