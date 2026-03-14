package org.example.learn.java.lang.spec.io.net.nio;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 *
 * 当 server socket 的 accept queue（全连接队列）非空时，内核就认为该 socket 可读,从而触发 accept 事件
 *
 * 为什么 ACCEPT 对应 READ
 *
 *
 * 为什么 OP_WRITE 几乎永远不应该注册?
 * TCP socket 在绝大多数时间都是“可写”的,因此 OP_WRITE 几乎一直处于 ready 状态.
 * 注册OP_WRITE会导致 Selector busy loop（CPU 空转）
 * 真实服务器的策略只有在数据未写完(实际写入的数据量少于希望写入的数据量)才注册OP_WRITE
 *
 *  select是一个阻塞操作,直到有selector上注册的SelectableChannel发生op-xxx ready 或者发生spurious-wakeup 或者当前线程被中断 或者wakeup方法被调用
 */
public class ServerSocketTest {

    public static final int PORT = 8080;

    /**
     * bio模式
     */
    @Test
    public void test01() throws Exception {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(PORT));

        while (true) {
            Socket socket = serverSocket.accept();      // 阻塞模式,accept返回值一定不为null
            System.out.println("socket.getRemoteSocketAddress() = " + socket.getRemoteSocketAddress());
            socket.close();
        }
    }

    /**
     * selectedKeys集合中的元素代表了本次select操作收集的read-op事件
     * 如果在下次select操作前,不将本次收集的事件清理掉,会一直保留在selectedKeys集合中.
     * 所以收到read-op事件并对事件做出响应后需要从selectedKeys中清除掉该事件.
     * 当然如果想暂时不处理该事件,可以让事件继续保留在selectedKeys中.
     *
     * 测试操作: 通过客户端工具建立一个tcp连接后,console会疯狂打印OP_ACCEPT ready
     */
    @Test
    public void test11() throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);                          // 如果serverSocketChannel是阻塞模式,就会报错. 因为selector不支持阻塞模式

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // 如果socket的全连接队列的连接没有通过accept被移走,每次select操作都不会发生阻塞
            // The number of keys, possibly zero, whose ready-operation sets were [updated] 注意: 除了第一次numKeys值为1,后续都是0,因为核心点在[updated],第一次select等到的key中op-accept ready了,后续select收集到该key中还是op-accept ready,没有发生变化
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            // numKeys是本次select收集到的新增事件.selectedKeys中保留的是到目前为止还未处理的事件(包含过去select收集到的暂未处理的事件)
            // 如果numKeys==0而selectedKeys().size>0 即表示有历史积压的事件未处理
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                // 因为没有主动清理之前select收集的事件,该事件会一直保留在selectedKeys集合中
                if (key.isAcceptable()) {
                    System.out.println("OP_ACCEPT ready.");
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", ssc, ssc.hashCode());
                }
            }

            // 作为演示,强制循环有限次数
            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }

    /**
     *
     * 测试操作: 通过客户端工具建立一个tcp连接后,console会疯狂打印OP_ACCEPT ready
     */
    @Test
    public void test12() throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);                          // 如果serverSocketChannel是阻塞模式,就会报错. 因为selector不支持阻塞模式

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // 如果socket的全连接队列的连接没有通过accept被移走,每次select操作都不会发生阻塞
            // The number of keys, possibly zero, whose ready-operation sets were [updated] 注意: 除了第一次numKeys值为1,后续都是0,因为核心点在[updated],第一次select等到的key中op-accept ready了,后续select收集到该key中还是op-accept ready,没有发生变化
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            // numKeys是本次select收集到的新增事件.selectedKeys中保留的是到目前为止还未处理的事件(包含过去select收集到的暂未处理的事件)
            // 如果numKeys==0而selectedKeys().size>0 即表示有历史积压的事件未处理
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                // 因为没有主动清理之前select收集的事件,该事件会一直保留在selectedKeys集合中
                if (key.isAcceptable()) {
                    System.out.println("OP_ACCEPT ready.");
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", ssc, ssc.hashCode());
                }

//                key.interestOps(key.interestOps() & (~ SelectionKey.OP_ACCEPT));
                keys.remove();
            }

            // 作为演示,强制循环有限次数
            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }

    /**
     * OP_ACCEPT事件是内核通过判断socket的全连接队列是否不为空来决定的,当执行accept方法时,会从socket的全连接队列取走一个接队
     *
     *
     * 测试操作: 通过客户端工具建立一个tcp连接后,console只会打印一次OP_ACCEPT ready
     */
    @Test
    public void test13() throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);                          // 如果serverSocketChannel是阻塞模式,就会报错. 因为selector不支持阻塞模式

        while (true) {
            System.out.println("select start");
            int numKeys = selector.select();                                                                  // 如果socket的全连接队列为空,则该方法会一直阻塞等待新的established连接到来
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                if (key.isAcceptable()) {
                    System.out.println("OP_ACCEPT ready.");
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", ssc, ssc.hashCode());

                    ssc.accept();    // 如果只有一个连接,accept将socket的全连接队列中唯一的连接取走,导致下个select会被阻塞
                }
            }
        }
    }

    /**
     * nio模式
     */
    @Test
    public void testX() throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);                          // 如果serverSocketChannel是阻塞模式,就会报错. 因为selector不支持阻塞模式

        while (true) {
            selector.select();                                                                  // select是一个阻塞操作,直到有selector上注册的SelectableChannel发生op-xxx ready 或者发生spurious-wakeup
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    System.out.println("OP_ACCEPT ready");

                    System.out.println("key.channel().getClass() = " + key.channel().getClass());
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();              // 这样获得ServerSocketChannel更规范
                    SocketChannel socketChannel;

                    // 因为isAcceptable() == true,只是说明 某一时刻有连接到达,实际accept时socket有变(连接已经断开了/被其他线程提前accept走了)
                    // serverSocketChannel是非阻塞模式,accept返回值可能会是null.
                    // 一次accept事件,可能积累了很多连接.一次accept完所有连接,没必要等下个循环
                    while ((socketChannel = ssc.accept()) != null) {
                        SocketAddress remoteAddress = socketChannel.getRemoteAddress();
                        System.out.println("可accept :remoteAddress = " + remoteAddress);
                        socketChannel.close();
                    }
                }
            }
        }
    }
}
