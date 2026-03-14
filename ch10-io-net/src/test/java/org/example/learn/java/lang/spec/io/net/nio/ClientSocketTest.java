package org.example.learn.java.lang.spec.io.net.nio;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

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
public class ClientSocketTest {

    public static final int PORT = 8080;

    /**
     * 不改动interestOps,那么向epoll注册的事件类型集合就不会改变.
     * selectedKeys中的事件(即集合中的SelectionKey)如果不主动移除,会一直存在.
     * OP_CONNECT和OP_WRITE都在epoll看来是POLLOUT(即socket-output-buffer有写空间了),所以需要非常小心OP_CONNECT和OP_WRITE,不能长期注册这2个事件,否则cpu会空转
     *
     *
     * 测试操作: 通过工具建立一个tcp server,测试代码与server建立连接后,不要关闭连接. console会疯狂打印OP_CONNECT ready,且select()返回值只有第一次是1,后续都是0.
     */
    @Test
    public void test011() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(PORT));// non-blocking模式下,通常返回的都是false
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // OP_CONNECT对应epoll的POLLOUT(即socket-output-buffer有写空间了),只要本地socket不关闭不写满数据,那么一直有POLLOUT事件,所以不会发生阻塞
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                // 因为没有主动将OP_CONNECT从selectedKeys集合中移除,所以selectedKeys中会一直存在OP_CONNECT事件
                if (key.isConnectable()) {
                    System.out.println("OP_CONNECT ready.");
                    SocketChannel sc = (SocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", sc, sc.hashCode());
                }
            }

            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }

    /**
     * 没有调用finishConnect,OP_WRITE一直无法被正确捕捉到
     *
     *
     * 测试操作: 通过工具建立一个tcp server,测试代码与server建立连接后,不要关闭连接. console会疯狂打印OP_CONNECT ready,且select()返回值只有第一次是1,后续都是0.
     */
    @Test
    public void test021() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        boolean establishedInConnnectMethod = socketChannel.connect(new InetSocketAddress(PORT));// non-blocking模式下,通常返回的都是false
        socketChannel.register(selector, (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE));

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // OP_CONNECT对应epoll的POLLOUT(即socket-output-buffer有写空间了),只要本地socket不关闭不写满数据,那么一直有POLLOUT事件,所以不会发生阻塞
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                // 因为没有主动将OP_CONNECT从selectedKeys集合中移除,所以selectedKeys中会一直存在OP_CONNECT事件
                if (key.isConnectable()) {
                    System.out.println("OP_CONNECT ready.");
                    SocketChannel sc = (SocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", sc, sc.hashCode());
                }

                // 因为没有调用finishConnect,OP_WRITE一直无法被正确捕捉到
                if (key.isWritable()) {
                    System.out.println("OP_WRITE ready.");

                    if (!establishedInConnnectMethod) {
                        System.out.println("因为没有调用finishConnect,OP_WRITE 无法被正确捕捉到.");
                    } else {
                        System.out.println("极端情况,connect方法中连接建立了");
                    }
                }
            }

            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }

    @Test
    public void test022() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(PORT));// non-blocking模式下,通常返回的都是false
        socketChannel.register(selector, (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE));

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // OP_CONNECT对应epoll的POLLOUT(即socket-output-buffer有写空间了),只要本地socket不关闭不写满数据,那么一直有POLLOUT事件,所以不会发生阻塞
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                // 因为没有主动将OP_CONNECT从selectedKeys集合中移除,所以selectedKeys中会一直存在OP_CONNECT事件
                if (key.isConnectable()) {
                    System.out.println("OP_CONNECT ready.");
                    SocketChannel sc = (SocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", sc, sc.hashCode());
                    sc.finishConnect();
                }

                if (key.isWritable()) {
                    System.out.println("OP_WRITE ready.");
                    System.out.println("调用finishConnect后,OP_WRITE就能被正确捕捉到.");
                }
            }

            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }

    @Test
    public void test03() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(PORT));// non-blocking模式下,通常返回的都是false
        socketChannel.register(selector, (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE));

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // OP_CONNECT对应epoll的POLLOUT(即socket-output-buffer有写空间了),只要本地socket不关闭不写满数据,那么一直有POLLOUT事件,所以不会发生阻塞
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                // 因为没有主动将OP_CONNECT从selectedKeys集合中移除,所以selectedKeys中会一直存在OP_CONNECT事件
                if (key.isConnectable()) {
                    System.out.println("OP_CONNECT ready.");
                    SocketChannel sc = (SocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", sc, sc.hashCode());

                    sc.finishConnect();
                    // OP_CONNECT事件只会关心一次,后续不再关心
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));

                    Assert.assertTrue("更新interestOps,并不影响readyOps", key.isConnectable());
                    Assert.assertTrue("更新interestOps,并不影响readyOps", (key.readyOps() & SelectionKey.OP_CONNECT) != 0);
                }

                if (key.isWritable()) {
                    System.out.println("OP_WRITE ready.");
                    System.out.println("调用finishConnect后,OP_WRITE就能被正确捕捉到.");
                }

                // 当前key的所有事件类型都处理完了,需要将该key从selectedKeys移除,不然内层的while会空转
                keys.remove();
            }

            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }

    /**
     * 测试操作: 通过工具建立一个tcp server,测试代码与server建立连接后,server主动发送数据且不要关闭连接. console会疯狂打印OP_READ ready,且select()返回值只有第一次是1,后续都是0.
     */
    @Test
    public void test11() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(PORT));                                      // non-blocking模式下,通常返回的都是false
        socketChannel.register(selector, (SelectionKey.OP_CONNECT | SelectionKey.OP_READ));

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // 因为socket-input-buffer中的数据一直没有消费,所以每次epoll都能获得EPOLLIN事件
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                if (key.isConnectable()) {
                    System.out.println("OP_CONNECT ready.");
                    SocketChannel sc = (SocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", sc, sc.hashCode());

                    // 用过就用清理掉对应事件(具体指的是事件bit) 不要使用keys.remove(),这样会一次消费多个事件bit
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));
                    sc.finishConnect();                                                            // 一定要主动调用finishConnect,否则后续的read/write操作可能抛异常NotYetConnectedException
                }

                if (key.isReadable()) {
                    System.out.println("OP_READ ready.");
                }

                keys.remove();
            }

            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }

    /**
     * 测试操作: 通过工具建立一个tcp server,测试代码与server建立连接后,server主动发送数据且不要关闭连接. console不会疯狂打印OP_READ ready,且select()返回值只有第一次是1,后续都是0.
     */
    @Test
    public void test12() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(PORT));                                      // non-blocking模式下,通常返回的都是false
        socketChannel.register(selector, (SelectionKey.OP_CONNECT | SelectionKey.OP_READ));

        int maxCount = 20;
        int counter = 0;
        while (true) {
            System.out.println("select start");
            // 因为socket-input-buffer中的数据被消费完,所以epoll要一直阻塞到socket又收到新的数据才都能获得EPOLLIN事件
            int numKeys = selector.select();
            System.out.printf("select completed. numKeys=%d   selectedKeys.size=%d\n", numKeys, selector.selectedKeys().size());

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                if (key.isConnectable()) {
                    System.out.println("OP_CONNECT ready.");
                    SocketChannel sc = (SocketChannel) key.channel();
                    System.out.printf("key.channel() = %s. hashCode=%s\n", sc, sc.hashCode());

                    // 用过就用清理掉对应事件(具体指的是事件bit) 不要使用keys.remove(),这样会一次消费多个事件bit
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));
                    sc.finishConnect();                                                            // 一定要主动调用finishConnect,否则后续的read/write操作可能抛异常NotYetConnectedException
                }

                if (key.isReadable()) {
                    System.out.println("OP_READ ready.");

                    socketChannel.read(ByteBuffer.allocate(1024));                        // 消费掉socket-input-buffer中的数据
                }

                keys.remove();
            }

            counter++;
            if (counter >= maxCount) {
                break;
            }
        }
    }
}
