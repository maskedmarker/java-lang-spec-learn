package org.example.learn.java.lang.spec.io.net.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Linux 下无论 SO_TIMEOUT、socket.setSoTimeout()还是 JDBC 的 socketTimeout 都只控制读超时，无法控制 TCP 写超时.
 * 因此我们采用 非阻塞写 + Selector + 写事件轮询 + 定时器 的工业级方案
 *
 * TCP 写超时
 * send buffer 塞满后不会阻塞线程 (非阻塞写 + 多次 flush)
 */
public class TcpWriteWithTimeoutExample {

    private static final long WRITE_TIMEOUT_NANOS = 3_000_000_000L; // 3秒

    public static void main(String[] args) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress("127.0.0.1", 8080));

        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_CONNECT);

        long writeStart = 0;
        ByteBuffer writeBuffer = ByteBuffer.wrap("Hello from Java non-blocking write\n".getBytes());

        while (true) {
            selector.select(500);

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                SocketChannel sc = (SocketChannel) key.channel();

                // 建立连接
                if (key.isConnectable()) {
                    if (sc.finishConnect()) {
                        System.out.println("[CONNECTED]");
                        writeStart = System.nanoTime();
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                // 写数据（带超时控制）
                if (key.isWritable()) {
                    while (writeBuffer.hasRemaining()) {
                        int n = sc.write(writeBuffer);
                        if (n == 0) break; // sendbuf 满了，需要等下轮写

                        // 每次写部分后检查超时
                        if (System.nanoTime() - writeStart > WRITE_TIMEOUT_NANOS) {
                            System.err.println("[TIMEOUT] write exceeded timeout");
                            close(sc, selector);
                            return;
                        }
                    }

                    if (!writeBuffer.hasRemaining()) {
                        System.out.println("[WRITE OK]");
                        close(sc, selector);
                        return;
                    }
                }
            }

            // 超时兜底（避免 Selector 假唤醒导致超时不触发）
            if (writeStart != 0 && System.nanoTime() - writeStart > WRITE_TIMEOUT_NANOS) {
                System.err.println("[TIMEOUT] write exceeded timeout (fallback)");
                close(channel, selector);
                return;
            }
        }
    }

    private static void close(SocketChannel channel, Selector selector) {
        try { channel.close(); } catch (Exception ignored) {}
        try { selector.close(); } catch (Exception ignored) {}
        System.out.println("[CLOSED]");
    }
}
