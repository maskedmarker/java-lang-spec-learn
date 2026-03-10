package org.example.learn.java.lang.spec.io.net.socket;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 零拷贝
 *
 * 在Linux中, sendfile的底层核心支撑是sendpage,允许skb引用page-cache的一个page
 * Java世界中唯一“正宗”的sendfile入口就是FileChannel.transferTo
 *
 *
 * FileChannel
 *     │ transferTo()
 *     ▼
 * SocketChannel
 *     │ TCP
 *     ▼
 * Server Socket
 *
 * 在 Linux 上内核路径通常是
 * Disk → PageCache → SocketBuffer → NIC
 *            ↑
 *        sendfile()
 * 整个过程 不会把文件内容复制到用户态 Java heap
 */
public class ZeroCopySendTest {

    @Before
    public void setup() {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(9000);
                System.out.println("Server started on port 9000");

                Socket socket = serverSocket.accept();
                System.out.println("Client connected");

                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[8192];
                long total = 0;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    total += n;
                }
                System.out.printf("Received bytes: %d. 内容如下:\n", total);
                System.out.println(new String(buffer, StandardCharsets.UTF_8));

                socket.close();
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Test
    public void test01() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path filePath = Paths.get(cwd, "src/test/resources/sendfile.file");

        // 1. 打开文件 2. 建立 TCP 连接
        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);
             SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9000))) {

            socketChannel.configureBlocking(true);

            long position = 0;
            long size = fileChannel.size();

            // 为什么要 while 循环? sendfile允许部分发送,Kafka/Netty都是这样写的💯💯💯
            while (position < size) {
                long transferred = fileChannel.transferTo(position, size - position, socketChannel); // 3. 核心调用：这里在Linux上会进入sendfile()
                System.out.println("transferred = " + transferred);

                // socket buffer满/网络背压,阻塞模式下通常不会发生 💯💯💯
                if (transferred == 0) {
                    continue;
                }

                position += transferred;
            }

            System.out.println("sendfile completed, bytes = " + size);
        }
    }
}
