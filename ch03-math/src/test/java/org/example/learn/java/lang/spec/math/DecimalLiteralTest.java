package org.example.learn.java.lang.spec.math;

import org.junit.Assert;
import org.junit.Test;


public class DecimalLiteralTest {


    /**
     * java 支持使用科学计数法来书写数值
     *
     * [sign] significand [E|e] exponent
     */
    @Test
    public void test0() {
        double a = 1.23e5;   // 1.23 × 10^5 = 123000.0
        double b = 1.23E5;   // 同上，E/e 不区分大小写
        double c = 1e-3;     // 1 × 10^-3 = 0.001
        double d = 5.67e0;   // 5.67 × 10^0 = 5.67
        double e = -2.5e2;   // -2.5 × 10^2 = -250.0

        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println(d);
        System.out.println(e);
    }

    /**
     * java 提供了多种二进制形式来书写整数
     */
    @Test
    public void test1() {
        // 二进制的方式(带下划线分割)
        int a = 0b0000_0101;
        System.out.println("a = " + a);
        // 二进制的方式(不带下划线分割)
        int b = 0b00000101;
        Assert.assertEquals(a, b);

        int aa = 0b1111_1111_1111_1111_1111_1111_1111_1011;
        System.out.println("aa = " + aa);
        int bb = 0b11111111111111111111111111111011;
        Assert.assertEquals(aa, bb);
    }

    /**
     * java 提供了多种十进制形式来书写整数
     */
    @Test
    public void test2() {
        // 十进制的方式(带下划线分割)
        int a = 1_000_000;
        System.out.println("a = " + a);

        // 十进制的方式(不带下划线分割)
        int b = 1000000;
        Assert.assertEquals(a, b);

        int aa = -1_000_000;
        System.out.println("aa = " + aa);
        int bb = -1000000;
        Assert.assertEquals(aa, bb);
    }


    /**
     * java 提供了多种十六进制形式来书写整数
     */
    @Test
    public void test3() {
        // 十六进制的方式(带下划线分割)
        int a = 0x0000_0005;
        System.out.println("a = " + a);
        // 十六进制的方式(不带下划线分割)
        int b = 0x00000005;
        Assert.assertEquals(a, b);

        int aa = 0xffff_fffb;
        System.out.println("aa = " + aa);
        int bb = 0xfffffffb;
        Assert.assertEquals(aa, bb);
    }
}
