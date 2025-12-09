package org.example.learn.java.lang.spec.io.net.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * TCP 写队列 + 背压(backpressure)防 OOM
 * 有界队列 = 背压控制
 */
public class TcpClientWithBackpressureExample {

    private static final long WRITE_TIMEOUT_NANOS = 3_000_000_000L; // 3秒写超时
    private static final int MAX_QUEUE = 10_000; // 写队列限制，防止OOM

    private final SocketChannel channel;
    private final Selector selector;
    private final ArrayBlockingQueue<ByteBuffer> writeQueue = new ArrayBlockingQueue<>(MAX_QUEUE);
    private long writeStartTime = 0;

    public TcpClientWithBackpressureExample(String host, int port) throws Exception {
        this.channel = SocketChannel.open();
        channel.configureBlocking(false);
        this.selector = Selector.open();
        channel.connect(new InetSocketAddress(host, port));
        channel.register(selector, SelectionKey.OP_CONNECT);
    }

    /** 业务线程调用此方法发送数据(有界队列 = 背压控制) */
    public boolean send(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        boolean ok = writeQueue.offer(buf);
        if (!ok) {
            System.err.println("[BACKPRESSURE] write queue full -> rejecting message");
            return false;
        }

        SelectionKey selectionKey = channel.keyFor(selector);
        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup(); // 提醒 selector 有数据要写

        return true;
    }

    /** 入主循环(可放入独立线程) */
    public void eventLoop() throws Exception {
        while (true) {
            selector.select(500);

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (key.isConnectable()) {
                    finishConnect(key);
                }
                if (key.isWritable()) {
                    flushWrites(key);
                }
            }

            checkWriteTimeout();
        }
    }

    private void finishConnect(SelectionKey key) throws IOException {
        if (channel.finishConnect()) {
            System.out.println("[CONNECTED]");
        }
    }

    private void flushWrites(SelectionKey key) throws IOException {
        ByteBuffer buf;

        while ((buf = writeQueue.peek()) != null) {
            int n = channel.write(buf);

            if (n == 0) { // sendbuf 满了，下轮再写
                if (writeStartTime == 0) {
                    writeStartTime = System.nanoTime();
                }

                return;
            }

            if (!buf.hasRemaining()) { // 此条消息写完
                writeQueue.poll();
                writeStartTime = 0;
            }

            // 写超时检测
            if (writeStartTime != 0 &&
                    System.nanoTime() - writeStartTime > WRITE_TIMEOUT_NANOS) {
                System.err.println("[TIMEOUT] write exceeds timeout -> close");
                close();
                return;
            }
        }

        // 队列写空 → 暂时不监听 write 事件
        key.interestOps(SelectionKey.OP_CONNECT);
    }

    private void checkWriteTimeout() throws IOException {
        if (writeStartTime != 0 &&
                System.nanoTime() - writeStartTime > WRITE_TIMEOUT_NANOS) {
            System.err.println("[TIMEOUT] (fallback) -> close");
            close();
        }
    }

    private void close() {
        try { channel.close(); } catch (Exception ignore) {}
        try { selector.close(); } catch (Exception ignore) {}
        System.out.println("[CLOSED]");
    }

    public static void main(String[] args) throws Exception {
        TcpClientWithBackpressureExample client = new TcpClientWithBackpressureExample("127.0.0.1", 8080);

        // 启动 IO 线程
        new Thread(() -> {
            try {
                client.eventLoop();
            } catch (Exception ignored) {
            }
        }).start();

        // 模拟业务线程发消息
        for (int i = 0; i < 20000; i++) {
            boolean ok = client.send(("msg-" + i + "\n").getBytes());
            if (!ok) {
                System.out.println("BUSINESS: write rejected -> backpressure triggered");
                Thread.sleep(50);
            }
        }
    }
}
