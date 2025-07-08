package org.example.learn.java.lang.spec.io.net.nio;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioHttpClientDemoTest {

    public static final int PORT = 8080;
    public static final String HOST = "localhost";

    @Test
    public void test1() throws IOException {
        NioHttpClient httpClient = new NioHttpClient(HOST, PORT);
        httpClient.get("/hello");
    }

    @Test
    public void test2() throws IOException {
        NioHttpClient httpClient = new NioHttpClient(HOST, PORT);
        httpClient.post("/hello", "你好");
    }
}
