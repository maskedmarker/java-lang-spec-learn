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
import java.util.concurrent.TimeUnit;

public class NioEchoTcpServerTest {

    public static final int PORT = 8080;

    @Test
    public void test0() throws Exception {
        NioEchoTcpServer nioEchoTcpServer = new NioEchoTcpServer(PORT);
        nioEchoTcpServer.start();

        while (nioEchoTcpServer.isRunning()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
