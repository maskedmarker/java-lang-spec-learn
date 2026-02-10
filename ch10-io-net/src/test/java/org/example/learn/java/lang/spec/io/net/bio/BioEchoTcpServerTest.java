package org.example.learn.java.lang.spec.io.net.bio;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class BioEchoTcpServerTest {

    public static final int PORT = 8080;

    @Test
    public void test0() throws Exception {
        BioEchoTcpServer bioEchoTcpServer = new BioEchoTcpServer(PORT);
        bioEchoTcpServer.start();

        while (bioEchoTcpServer.isRunning()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
