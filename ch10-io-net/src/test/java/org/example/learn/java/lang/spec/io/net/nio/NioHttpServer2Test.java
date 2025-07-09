package org.example.learn.java.lang.spec.io.net.nio;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioHttpServer2Test {

    public static final int PORT = 8080;

    @Test
    public void test0() throws Exception {
        start();
        System.in.read();
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


    private void handleHttp() throws Exception {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        // 先向selector注册
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 再bind
        serverChannel.bind(new InetSocketAddress(PORT));


        System.out.println("HTTP Server started at http://localhost:" + PORT);

        while (true) {
            selector.select(); // 阻塞直到有事件发生
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (key.isAcceptable()) {
                    // netty将serverSocketChannel accept到的socketChannel称呼为childChannel
                    SocketChannel childChannel = serverChannel.accept();
                    childChannel.configureBlocking(false);
                    childChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    try {
                        handleRequest(key);
                    } catch (IOException e) {
                        // 捕获并打印异常,但是不能抛出
                        e.printStackTrace();
                    }
                } else if (!key.isValid()) {
                    key.cancel();
                }
            }
        }
    }

    private void handleRequest(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        // 不需要手动 remove，close() 会自动取消注册
        int read = client.read(buffer);
        if (read == -1) {
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
