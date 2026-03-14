# Selector

高版本的java有更多的源码信息.请参考java-lang-spec-learn-new这个项目的对应部分.

```text
select操作在操作系统层面,是怎么判断有op_accept ready?

当 server socket 的 accept queue(全连接队列)非空时,内核就认为该 socket 可读,从而触发 accept 事件.
```

```text
为什么 ACCEPT 对应 READ

很多人第一次看到会疑惑：OP_ACCEPT 为什么底层是 EPOLLIN

在 TCP 语义中：
listen socket readable 表示 有 pending connection.
也就是说：listen socket 的 "read data" 其实是 "新的连接"

因此
Socket类型	     EPOLLIN含义
client socket	 有数据可读
server socket	 有连接可 accept
```


```text
当非阻塞 connect 的 TCP 握手完成(成功或失败)时,socket 会变为 writable,从而触发 OP_CONNECT.

TCP 三次握手完成时,socket 状态改变,内核会触发 socket 状态通知.

为什么 connect 完成会变成 writable?
socket writable表示发送缓冲区可以发送数据, 
而在 connect 完成前,发送路径不可用, 当三次握手完成,发送路径建立socket writable, 因此EPOLLOUT ready.

连接失败也会触发 OP_CONNECT
非常重要的一点,OP_CONNECT ready,并不保证连接成功.所以必须调用channel.finishConnect()来确认结果.

finishConnect() 做什么
finishConnect() 会检查SO_ERROR


为什么 OP_CONNECT 只触发一次


很多人会疑惑：OP_CONNECT 和 OP_WRITE 都对应 EPOLLOUT
区别在于：
阶段	             Java事件
connect 未完成	 OP_CONNECT
connect 完成	     OP_WRITE
Java NIO 内部会根据 socket 状态区分.


为什么 OP_CONNECT 和 OP_WRITE 都映射到底层的 EPOLLOUT,但 Java NIO 却能区分两者？

关键原因是：JDK 在 SocketChannelImpl 中维护了连接状态机,并结合 EPOLLOUT 事件进行二次判定.

在 SocketChannelImpl 内部维护了状态
UNCONNECTED
     |
     | connect()
     v
CONNECTION_PENDING
     |
     | finishConnect()
     v
CONNECTED
```



```text
为什么 OP_WRITE 几乎永远不应该注册？

TCP socket 在绝大多数时间都是“可写”的,因此 OP_WRITE 几乎一直处于 ready 状态.
这会导致 Selector busy loop(CPU 空转).

真实服务器的策略
都遵循同一个模式：
默认只监听

OP_ACCEPT
OP_READ

只有在：write buffer 未写完,才注册OP_WRITE,写完立即取消 OP_WRITE

所以设计原则是
OP_WRITE 只在 write 不完时临时注册,而不是长期监听.
```