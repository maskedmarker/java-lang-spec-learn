package org.example.learn.java.lang.spec.io.net.socket;

import org.junit.Assert;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 解析域名
 */
public class SocketAddressTest {

    /**
     * 实时解析域名
     */
    @Test
    public void test01() throws UnknownHostException {
        // 通过向DNS服务发送请求来将hostname解析为ip地址
        InetAddress[] allInetAddress = InetAddress.getAllByName("www.baidu.com");
        Arrays.stream(allInetAddress).forEach(System.out::println);

        // 从解析的ip中,挑选第一个
        InetAddress inetAddress = InetAddress.getByName("www.baidu.com");
        System.out.println("挑选第一个ip地址 = " + inetAddress);

        // ip地址对应的类是InetAddress(子类是Inet4Address和Inet6Address)
        Arrays.stream(allInetAddress).map(i-> i.getClass()).forEach(System.out::println);
        Arrays.stream(allInetAddress).allMatch(i -> i instanceof Inet4Address || i instanceof Inet6Address);
    }

    /**
     * socket的地址需要ip+port才行
     *
     * 只有通过getByName类方法才能获取到InetAddress对象,此时会发生DNS解析.
     * 而InetSocketAddress对象又依赖InetAddress对象.
     * 如果要获取一个InetSocketAddress对象还不触发DNS解析,那么只有一种方式获取(createUnresolved)
     */
    @Test
    public void test02() throws UnknownHostException {
        // 只有一种方法创建unresolved socket address
        InetSocketAddress unresolved = InetSocketAddress.createUnresolved("www.baidu.com", 80);
        Assert.assertNull("未解析的InetSocketAddress会返回null", unresolved.getAddress());


        // 内部会InetAddress.getByName,触发DNS同步解析
        InetSocketAddress resolved = new InetSocketAddress("www.baidu.com", 80);
        Assert.assertNotNull("已解析的InetSocketAddress不会返回null", resolved.getAddress());
    }
}
