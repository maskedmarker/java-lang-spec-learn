package org.example.learn.java.lang.spec.io.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 当你调用 SocketChannel.close() 时，是否还需要从 Selector 中手动移除对应的 SelectionKey？
 * 不需要手动 remove，close() 会自动取消注册。
 *
 * 调用SocketChannel.close方法会将其SelectionKey设置为cancelled,即该SelectionKey会被放到Selector的cancelled-key,在下次select的时候被自动清除
 * SocketChannel.close会触发OS的网络栈发送tcp的FIN报文
 *
 * key.isReadable()
 * socket的input-buffer有数据了,或者对方发起了关闭tcp连接(即对方发送了FIN,本方OS回应ACK)
 * 对方发起了关闭tcp可以看作是发送了"不再发送数据"的命令信息(而非数据信息)
 *
 * selector.selectedKeys()
 * A key may be removed directly from the selected-key set by invoking the set's remove method or by invoking the remove method of an iterator obtained from the set.
 */
public class NioEchoTcpServer {

    private final int port;

    private AtomicBoolean running = new AtomicBoolean(false);

    ServerSocketChannel serverChannel;

    Selector selector;

    Map<SocketChannel, InetSocketAddress> connections = new HashMap<>();


    public NioEchoTcpServer(int port) {
        this.port = port;
    }

    public void start() {
        // 单独启动一个io线程
        Thread ioThread = new Thread(() -> {
            try {
                this.initSocket();
                this.handleTcpRequest();
            } catch (Exception e) {
                System.out.println("NioTcpServer发生异常");
                e.printStackTrace();
            }
        });
        ioThread.start();
        running.set(true);
    }

    public void stop() {
        running.set(false);
    }
    public boolean isRunning() {
        return running.get();
    }

    private void initSocket() throws IOException {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(this.port);

        // 先完成bind再向selector注册
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(inetSocketAddress);
        this.serverChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.printf("Tcp Server started at %s\n", inetSocketAddress);
    }

    private void handleTcpRequest() throws Exception {
        while (running.get()) {
            // 阻塞直到有事件发生.(os和jvm完全隐藏了tcp的底层细节. 比如隐藏了tcp3次握手的过程和tcp收到数据后的ack自动应答)
            this.selector.select();
            // 每次select()方法结束之后,就会有一个全新的selectedKeys对象(类似于不同的快照对象),selectedKeys的元素只能通过remove方法来删除
            Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                // jdk使用的是水平触发(Level-Triggered)机制, 所以处理完key的事件后,必须要将其从事件集selectedKeys中移除,不然就出现无意义的循环了.
                keys.remove();

                if (key.isAcceptable()) {
                    // netty将serverSocketChannel accept到的socketChannel称呼为childChannel
                    SocketChannel childChannel = serverChannel.accept();
                    childChannel.configureBlocking(false);
                    SelectionKey selectionKey = childChannel.register(this.selector, SelectionKey.OP_READ);
                    selectionKey.attach(new ArrayList<>()); // 收集接收到的数据

                    System.out.printf("建立新连接: %s -> %s\n", childChannel.getRemoteAddress(), this.serverChannel.getLocalAddress());
                } else if (key.isReadable()) { // socket的input-buffer有数据了,或者对方发起了关闭tcp连接(即对方发送了FIN,本方OS回应ACK)
                    try {
                        handleRequest(key);
                    } catch (IOException e) {
                        // 发生异常,抛弃该连接
                        key.cancel();
                    }
                } else if (!key.isValid()) {
                    // 调用SocketChannel.close方法会将其SelectionKey设置为cancelled,即该SelectionKey会被放到Selector的cancelled-key,在下次select的时候被自动清除
                    // invalid的原因: until it is cancelled, its channel is closed, or its selector is closed. 前2种情况selector会自动清理,最后的情况更不用处理.
                    // 这里是多余的
                    key.cancel();
                }
            }
        }
    }


    /**
     *  💯💯💯ByteBuffer写完数据后一定要主动flip,方便使用方读取.
     */
    private void handleRequest(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        List<ByteBuffer> buffers = new ArrayList<>();
        ByteBuffer tmp = ByteBuffer.allocate(1024); // 初始是写模式
        int nRead;
        while ((nRead = client.read(tmp)) > 0) {
            if (!tmp.hasRemaining()) {
                tmp.flip();  // 切换到读模式,方便使用者💯
                buffers.add(tmp);
                tmp = ByteBuffer.allocate(1024);
            }
        }
        if (tmp.position() != 0) {
            tmp.flip();  // 切换到读模式,方便使用者💯
            buffers.add(tmp);
        }
        // 收集接收到的数据
        if (buffers.size() > 0) {
            ByteBuffer data = merge(buffers); // 因为之前已经flip了,此时buffers都处于读模式
            @SuppressWarnings("unchecked") List<ByteBuffer> attachment = (List<ByteBuffer>) key.attachment();
            attachment.add(data);

            // 输出接收到的字节
            System.out.println("接收到请求:");
            String request = new String(data.array(), StandardCharsets.UTF_8);
            System.out.println(request);
        }


        // 如果对端先发起close
        if (nRead == -1) {
            // nio设计中,只有本方发送了FIN才被认作是closed(当前仅仅是收到了对方发送的FIN,现在是半双工)
            assert !client.socket().isClosed();
            ByteBuffer allData = merge((List<ByteBuffer>) key.attachment());
            client.write(allData);

            System.out.printf("关闭连接: %s -> %s\n", client.getRemoteAddress(), client.getLocalAddress());
            // 发送tcp的FIN报文,完成双工的本侧关闭工作
            client.close();
            assert client.socket().isClosed();
        }
    }

    private static ByteBuffer merge(List<ByteBuffer> buffers) {
        int total = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
        ByteBuffer merged = ByteBuffer.allocate(total);
        for (ByteBuffer b : buffers) {
            merged.put(b.duplicate());  // duplicate复用底层数据同时不改变position/limit,put还是会发生数据复制
        }

        merged.flip();   // 切换到读模式,方便使用者💯
        return merged;
    }
}
