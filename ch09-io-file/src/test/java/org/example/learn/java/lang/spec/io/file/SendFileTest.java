package org.example.learn.java.lang.spec.io.file;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * sendfile是一种内核态直接数据传输机制,用于将文件内容从磁盘直接发送到网络 socket,避免在用户态与内核态之间进行多次数据拷贝.
 * 在Linux中, sendfile的底层核心支撑是sendpage,允许skb引用page-cache的一个page
 * <p>
 * sendfile系统调用的简化:
 * -> 触发系统调用,进入内核态
 * -> 基于文件句柄fd获取内存页page (如果此时发生page-fault,则触发page加载机制, DMA加载文件页)
 * -> 创建skb,并引用page地址指针
 * -> skb入队socket发送队列
 * -> 结束系统调用,返回用户态
 *
 * Java世界中唯一“正宗”的sendfile入口就是FileChannel.transferTo
 */
public class SendFileTest {

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

    @Test
    public void test02() throws IOException {
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
