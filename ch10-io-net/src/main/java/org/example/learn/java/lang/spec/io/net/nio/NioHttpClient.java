package org.example.learn.java.lang.spec.io.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioHttpClient {

    public final int port;
    public final String host;

    public NioHttpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void get(String url) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));

        String request = "GET " + url + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        socketChannel.write(ByteBuffer.wrap(request.getBytes()));
        // 没有设置成non-blocking模式
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (socketChannel.read(buffer) > 0) {
            buffer.flip();
            System.out.print(new String(buffer.array(), 0, buffer.limit()));
            buffer.clear();
        }

        socketChannel.close();
    }

    public void post(String url, String msg) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));

        String request = "POST " + url + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                msg;

        socketChannel.write(ByteBuffer.wrap(request.getBytes()));
        // 没有设置成non-blocking模式
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (socketChannel.read(buffer) > 0) {
            buffer.flip();
            System.out.print(new String(buffer.array(), 0, buffer.limit()));
            buffer.clear();
        }

        socketChannel.close();
    }
}
