package org.example.learn.java.lang.spec.math;

import org.junit.Test;

/**
 * Binary Representation
 * Binary literals can only be used with integral types.
 * The value of the binary literal must fit within the range of the specified data type. If it exceeds, a compilation error occurs.
 *
 * Binary literals are signed by default in Java
 * Binary for Positive Numbers
 *      Positive numbers are represented directly in binary, padded with leading 0s to fit 8 bits
 * Binary for Negative Numbers
 *      Negative numbers use two's complement representation.
 * 最左digit是1时,会被当作two's complement representation;
 * 最左digit不是1时,不会被当作two's complement representation;
 */
public class BitRepresentationTest {

    @Test
    public void test0() {
        byte b = 0b0000_0001;
        System.out.println("十进制值: 0b0000_0001 = " + b);

        // compiler会根据变量类型自动补0
        short s = 0b1000_0000;
        short ss = 0b0000_0000_1000_0000; // 手动补0
        System.out.println("十进制值: 0b1000_0000 = " + s);
        System.out.println("十进制值: 0b0000_0000_1000_0000 = " + ss);

        // 默认情况下,被当作int类型;如果需要,则需要添加L声明是long类型
        System.out.println(0b1000_0000); // 注意观察调用的哪个重载的方法
        System.out.println(0b1000_0000L);

        // 多余的leading zeros会被忽略
        byte bb = 0b0000_0000_0000_1000;
        System.out.println("十进制值: 0b0000_0000_0000_1000 = " + bb);
    }

    /**
     * 负数的Binary Representation与正数不同.
     * 是以two's complement representation(补码方式表达的)
     *
     */
    @Test
    public void test10() {
        // 1的写法
        byte b = 0b0000_0001;
        System.out.println("b = " + b);
        // -1的写法
        byte b1 = (byte)0b1111_1111;
        System.out.println("(byte)0b1111_1111 = " + b1);
    }

    /**
     * 虽然还未找到可靠的spec规范说明.可以这样理解:
     * 所有的binary literal都当作int值的二进制表示法;当结尾带L时,都当作long值的二进制表示法.
     *
     */
    @Test
    public void test11() {
        /* 虽然还未找到spec规范, 如下可以这样理解:
        * 当没有添加(byte)时, compiler将0b1111_1111理解为int值(即255),此时int值大于byte类型的数值范围,compiler报错
        * 添加(byte)时,将发生截断,保留最后的8个bit,且这8个bit序列符合-1的补码规则
        */

        // -1的写法
        byte b = (byte)0b1111_1111;
        System.out.println("(byte)0b1111_1111 = " + b);
        int i = 0b1111_1111;
        System.out.println("0b1111_1111 = " + i);
    }
}
