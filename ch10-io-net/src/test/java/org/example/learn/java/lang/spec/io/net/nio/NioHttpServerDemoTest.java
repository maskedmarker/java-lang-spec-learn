package org.example.learn.java.lang.spec.io.net.nio;

import org.junit.Test;

public class NioHttpServerDemoTest {

    public static final int PORT = 8080;

    @Test
    public void test0() throws Exception {
        NioHttpServer httpServer = new NioHttpServer(PORT);
        httpServer.start();

        System.in.read();
        httpServer.stop();
    }
}
