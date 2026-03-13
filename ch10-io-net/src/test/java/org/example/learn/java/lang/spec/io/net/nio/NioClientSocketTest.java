package org.example.learn.java.lang.spec.io.net.nio;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 在非阻塞模式下,SocketChannel.connect()仅仅是一个发起连接的请求.其返回true或false表示连接请求是否完成(返回true的情况很罕见)
 * 后续想直到连接请求是否完成,需要通过SocketChannel.finishConnect()的返回值来确定
 */
public class NioClientSocketTest {

    public static final int PORT = 8080;
    public static final byte[] DATA = "hello world".getBytes(StandardCharsets.UTF_8);

    @Before
    public void setup() throws Exception {
        NioEchoTcpServer nioEchoTcpServer = new NioEchoTcpServer(PORT);
        nioEchoTcpServer.start();

        while (nioEchoTcpServer.isRunning()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void test0() throws Exception {
        // 创建非阻塞SocketChannel
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);

        Selector selector = Selector.open();

        // 💯💯💯 发起连接请求 If this channel is in non-blocking mode then an invocation of this method initiates a non-blocking connection operation.
        if (channel.connect(new InetSocketAddress(PORT))) {
            // 连接立即建立（罕见情况）
            System.out.println("立即连接成功");
        } else {
            System.out.println("连接进行中,需要等待");
            channel.register(selector, SelectionKey.OP_CONNECT);
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();

                for (SelectionKey key : keys) {
                    SocketChannel sc = (SocketChannel) key.channel();
                    if (key.isConnectable()) {
                        System.out.println("可以建立新连接");

                        // 💯💯💯虽然被select检测OP_CONNECT,但在read/write前还是要call finishConnect(),否则有抛出NotYetConnectedException异常的可能
                        if (sc.finishConnect()) {
                            System.out.println("连接完成");
                            sc.register(selector, SelectionKey.OP_READ); // 需要移除OP_CONNECT,否则建立连接后select()还会返回该channel. 添加OP_READ
                        } else {
                            throw new Error("selector发现socketChannel已经OP_CONNECT ready,但是finishConnect()却返回false,意味着连接还未建立");
                        }

                        // jdk使用的是水平触发(Level-Triggered)机制,如果不移除OP_CONNECT,后续每次select还会返回当前连接
                        int ops = key.interestOps();
                        ops &= ~SelectionKey.OP_CONNECT;
                        key.interestOps(ops);
                    } else if (key.isWritable()) {
                        System.out.println("可以写入数据");
                        sc.write(ByteBuffer.wrap(DATA));
                    } else if (key.isReadable()) {
                        System.out.println("可以读取数据");
                        read(sc);
                    }
                }
            }
        }
    }

    /**
     *  💯💯💯ByteBuffer写完数据后一定要主动flip,方便使用方读取.
     */
    private void read(SocketChannel socketChannel) throws IOException {
        List<ByteBuffer> buffers = new ArrayList<>();

        ByteBuffer tmp = ByteBuffer.allocate(1024); // 初始是写模式
        int nRead;
        while ((nRead = socketChannel.read(tmp)) > 0) {
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
        ByteBuffer data = merge(buffers); // 因为之前已经flip了,此时buffers都处于读模式
        // 输出接收到的字节
        System.out.println("接收到请求:");
        String request = new String(data.array(), StandardCharsets.UTF_8);
        System.out.println(request);

        // 如果对端先发起close
        if (nRead == -1) {
            // nio设计中,只有本方发送了FIN才被认作是closed(当前仅仅是收到了对方发送的FIN,现在是半双工)
            assert !socketChannel.socket().isClosed();
            System.out.printf("关闭连接: %s -> %s\n", socketChannel.getRemoteAddress(), socketChannel.getLocalAddress());
            // 发送tcp的FIN报文,完成双工的本侧关闭工作
            socketChannel.close();
            assert socketChannel.socket().isClosed();
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
