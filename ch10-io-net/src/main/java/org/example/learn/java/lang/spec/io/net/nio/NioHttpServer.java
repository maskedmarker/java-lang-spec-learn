package org.example.learn.java.lang.spec.io.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
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
public class NioHttpServer {

    private final int port;

    private AtomicBoolean stopRunning = new AtomicBoolean(false);

    public NioHttpServer(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(() -> {
            try {
                this.handleHttp();
            } catch (Exception e) {
                System.out.println("NioHttpServer发生异常");
                e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        stopRunning.set(true);
    }

    private void handleHttp() throws Exception {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        // 先完成bind
        serverChannel.bind(new InetSocketAddress(this.port));
        serverChannel.configureBlocking(false);

        Selector selector = Selector.open();
        // 先完成bind再向selector注册
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("HTTP Server started at http://localhost:" + this.port);

        while (!stopRunning.get()) {
            // 阻塞直到有事件发生.(os和jvm完全隐藏了tcp的底层细节. 比如隐藏了tcp3次握手的过程和tcp收到数据后的ack自动应答)
            selector.select();
            // 每次select()方法结束之后,就会有一个全新的selectedKeys对象(类似于不同的快照对象),selectedKeys的元素只能通过remove方法来删除
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                if (key.isAcceptable()) {
                    // netty将serverSocketChannel accept到的socketChannel称呼为childChannel
                    SocketChannel childChannel = serverChannel.accept();
                    childChannel.configureBlocking(false);
                    childChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) { // socket的input-buffer有数据了,或者对方发起了关闭tcp连接(即对方发送了FIN,本方OS回应ACK)
                    try {
                        handleRequest(key);
                    } catch (IOException e) {
                        // 捕获并打印异常,但是不能抛出
                        e.printStackTrace();
                    }
                } else if (!key.isValid()) {
                    // 调用SocketChannel.close方法会将其SelectionKey设置为cancelled,即该SelectionKey会被放到Selector的cancelled-key,在下次select的时候被自动清除
                    // invalid的原因: until it is cancelled, its channel is closed, or its selector is closed. 前2种情况selector会自动清理,最后的情况更不用处理.
                    // 这里是多余的
                    key.cancel();
                }

                // jdk使用的是水平触发(Level-Triggered)机制, 所以处理完key的事件后,必须要将其从事件集selectedKeys中移除,不然就出现无意义的循环了.
                keys.remove();
            }
        }
    }

    /**
     *
     */
    private void handleRequest(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = client.read(buffer);
        if (read == -1) {
            // nio设计中,只有发送了FIN才被认作是closed
            assert !client.socket().isClosed();
            // 发送tcp的FIN报文,完成双工的本侧关闭工作
            client.close();
            return;
        }

        buffer.flip();
        String request = new String(buffer.array(), 0, buffer.limit());
        System.out.println("接收到请求:\n" + request);

        // 简单 HTTP 响应
        String responseBody = "Hello from NIO HTTP Server!";
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + responseBody.getBytes().length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                responseBody;

        client.write(ByteBuffer.wrap(httpResponse.getBytes()));
        client.close();
    }
}
