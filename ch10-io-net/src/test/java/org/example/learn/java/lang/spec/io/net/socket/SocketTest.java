package org.example.learn.java.lang.spec.io.net.socket;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * 解析域名
 */
public class SocketTest {

    @Test
    public void test01() throws UnknownHostException {
        // 只有一种方法创建unresolved socket address
        InetSocketAddress unresolved = InetSocketAddress.createUnresolved("www.example.com", 443);
        InetAddress address1 = unresolved.getAddress();
        System.out.println("address1 = " + address1);
        Assert.assertNull("未解析的InetSocketAddress会返回null", address1);


        // 内部会InetAddress.getByName,触发DNS同步解析
        InetSocketAddress resolved = new InetSocketAddress("www.example.com", 443);
        InetAddress address2 = resolved.getAddress();
        System.out.println("address2 = " + address2);
        Assert.assertNotNull("已解析的InetSocketAddress不会返回null", address2);
    }


    /**
     * socket的地址需要ip+port才行
     *
     */
    @Test
    public void test02() throws IOException {
        String host = "www.example.com";
        int port = 80;


        String httpRequest = "GET / HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Connection: close\r\n\r\n";


        // Creates a stream socket and connects it to the specified port number at the specified IP address
        Socket socket = new Socket(InetAddress.getByName(host), port);
        try (InputStream inputStream = socket.getInputStream(); OutputStream outputStream = socket.getOutputStream()) {
            // 发送请求
            outputStream.write(httpRequest.getBytes(StandardCharsets.UTF_8));

            // 接收请求
            byte[] buffer = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(buffer)) > 0 ) {
                String content = new String(buffer, 0, nRead, StandardCharsets.UTF_8);
                System.out.print(content);
            }
        }
    }
}
