package org.example.learn.java.lang.spec.io.net.socket;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 当server端不去取走(accept) backlog队列中已经建立的全连接时,新的连接就无法建立(client端的SYN请求会被server端以RST+ACK拒绝)
 *  socket接口中的backlog参数指的是允许server端可以积压一定量未处理的全连接.(准确讲backlog参数控制全连接队列的大小.至于半连接的队列的大小,由操作系统的其他参数控制)
 *
 *
 *  在server启动阶段,
 * 从bind端口到select/accept调用这段时间内,新来的连接可能已经占满了backlog队列,导致新来请求无法建立连接.
 */
public class TcpBacklogTest {

    public static final int PORT = 8080;

    // The maximum number of pending connections (支持最多多少个待处理的)
    public static final int BACKLOG = 3;

    private ServerSocketChannel serverChannel;
    private ServerSocket serverSocket;


    @Before
    public void setupTcpServer() {
        // 单独启动一个io线程
        Thread ioThread = new Thread(() -> {
            try {
                this.openSocketAndDoNotAccept();

                while (!serverSocket.isClosed()) {
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (Exception e) {
                System.out.println("NioTcpServer发生异常");
                e.printStackTrace();
            }
        });
        ioThread.start();
    }

    private void openSocketAndDoNotAccept() throws IOException {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(PORT);
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(inetSocketAddress, BACKLOG);
        this.serverSocket = serverChannel.socket();

        System.out.printf("Tcp Server started at %s, BUT it do not accept any connection\n", inetSocketAddress);
    }

    /**
     * 可以顺利建立BACKLOG个连接(即完成tcp三次握手),
     * 第BACKLOG+1个连接在建立时,client端发送的SYN请求会被拒绝,server端返回RST+ACK
     */
    @Test
    public void test0() {
        int connectTimeout = 1000;
        int nClient = BACKLOG + 3;
        List<Socket> clients = new ArrayList<>();
        for (int i = 0; i < nClient; i++) {
            try {
                Socket socket = new Socket();
                System.out.printf("第%d个tcp客户端尝试连接服务端\n", (i+1));
                socket.connect(new InetSocketAddress(PORT), connectTimeout);
                System.out.printf("第%d个tcp客户端尝试连接服务端 成功\n", (i+1));
                clients.add(socket);
            } catch (IOException e) {
                System.out.printf("第%d个tcp客户端尝试连接服务端 失败. %s\n", (i+1), e);
                Assert.assertTrue("", (i + 1) > BACKLOG);
            }
        }

    }
}
