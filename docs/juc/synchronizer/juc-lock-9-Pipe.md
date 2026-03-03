# juc-Pipe

Pipe的单向传递字节数据(而非对象).
Pipe不保证多reader/writer的并发安全,所以Pipe应该只有一对reader/writer使用同一个pipe对象,或者多reader/writer在使用时加锁. (Exchanger保证多reader/writer并发安全)
(由于Pipe基于SocketChannelImpl实现的SourceChannel/SinkChannel,而SocketChannelImpl在read/write时是会添加读写锁的,所以reader/writer实际是并发安全的)

```text
A pipe consists of a pair of channels: A writable sink channel and a readable source channel. 
Once some bytes are written to the sink channel they can be read from source channel in exactly AT the order in which they were written.

Whether or not a thread writing bytes to a pipe will block until another thread reads those bytes, or some previously-written bytes, from the pipe is system-dependent and therefore unspecified. 
Many pipe implementations will buffer up to a certain number of bytes between the sink and source channels, but such buffering should not be assumed.
```

```text
Pipe的A writable sink channel and a readable source channel是通过在loop-back网卡上创建2个tcp端口实现的.
一个端口作为client端,一个端口作为server端.

注意这是windows平台的实现. linux平台的Pipe实现是基于内核pipe(2).
原因在于Windows没有Unix风格的匿名pipe可直接用于select,Windows的select()只对socket有效,而Linux的pipe支持select().
```