package org.example.learn.java.lang.spec.math.bit;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * 对于int类型的位运算符左操作数, 位运算符右操作数为了保证不能大于31,会对位运算符右操作数mod-32
 * 对于long类型的位运算符左操作数, 位运算符右操作数为了保证不能大于63,会对位运算符右操作数mod-64
 */
public class BitMoveTest {

    @Test
    public void test0() {
        byte b1= 0b00000001;
        byte b2= 0b00000010;
        byte b3= 0b00000100;
        byte b4= 0b00001000;
        byte b5= 0b00010000;
        byte b6= 0b00100000;
        byte b7= 0b01000000;

        // 左移n,等价于乘以2^n
        System.out.println("(b1 << 0) = " + (b1 << 0));
        System.out.println("(b1 << 8) = " + (b1 << 8));
        System.out.println("(b2 << 8) = " + (b2 << 8));
        System.out.println("(b6 << 8) = " + (b6 << 8));
        System.out.println("(b7 << 8) = " + (b7 << 8));

        System.out.println("Integer.toBinaryString((b1 << 0)) = " + Integer.toBinaryString((b1 << 0)));
        System.out.println("Integer.toBinaryString((b1 << 8)) = " + Integer.toBinaryString((b1 << 8)));
        System.out.println("Integer.toBinaryString((b2 << 8)) = " + Integer.toBinaryString((b2 << 8)));
        System.out.println("Integer.toBinaryString((b6 << 8)) = " + Integer.toBinaryString((b6 << 8)));
        System.out.println("Integer.toBinaryString((b7 << 8)) = " + Integer.toBinaryString((b7 << 8)));

        // 📌 对于int类型的位运算符左操作数, 位运算符右操作数为了保证不能大于32,会对位运算符右操作数mod-32
        System.out.println("Integer.toBinaryString(b1 << 31) = " + Integer.toBinaryString(b1 << 31));
        System.out.println("Integer.toBinaryString((b1 << 32)) = " + Integer.toBinaryString((b1 << 32)));
        System.out.println("Integer.toBinaryString((b1 << 33)) = " + Integer.toBinaryString((b1 << 33)));
        Assert.assertEquals("对于int类型的位运算符左操作数, 位运算符右操作数为了保证不能大于32,会对位运算符右操作数mod 32", (b1 << 0), (b1 << (32%32)));
        Assert.assertEquals("对于int类型的位运算符左操作数, 位运算符右操作数为了保证不能大于32,会对位运算符右操作数mod 32", (b1 << 1), (b1 << (33%32)));
        System.out.println("Integer.toBinaryString((b1 << 32)) = " + Integer.toBinaryString(((b1 << 31) << 3)));

        // 📌 对于long类型的位运算符左操作数, 位运算符右操作数为了保证不能大于64,会对位运算符右操作数mod-64
        long l = 0b00000001;
        System.out.println("Long.toBinaryString((l << 32)) = " + Long.toBinaryString((l << 32)));
        Assert.assertNotEquals((b1 << 32), (l << 32));
        Assert.assertEquals((b1 << 0), (l << 64));
    }

    /**
     * << 太多时,可以使用BigInteger(arbitrary-precision integers)
     */
    @Test
    public void test1() {

        BigInteger bigInteger = BigInteger.ONE.shiftLeft(100);
        System.out.println("(1 << 100) = " + bigInteger);
        Assert.assertEquals(new BigInteger("1267650600228229401496703205376"), BigInteger.ONE.shiftLeft(100));
    }

    @Test
    public void test2() {
        final int countBits = Integer.SIZE - 3;
        System.out.println("countBits = " + countBits);

        int running = -1 << countBits; // -1的补码各个位都是1,此时左移多少位,相当于最右多少个位改为0. 此处等价于高三位都为1,其余为0
        System.out.println("Integer.toBinaryString(running) = " + Integer.toBinaryString(running));

        final int capacity   = (1 << countBits) - 1; // 等价于是最低的countBits个位都是1
        System.out.println("Integer.toBinaryString(capacity) = " + Integer.toBinaryString(capacity));
    }

    @Test
    public void test22() {
        int COUNT_BITS = Integer.SIZE - 3;
        int CAPACITY   = (1 << COUNT_BITS) - 1;
        // runState is stored in the high-order bits
        int RUNNING    = -1 << COUNT_BITS;
        int SHUTDOWN   =  0 << COUNT_BITS;
        int STOP       =  1 << COUNT_BITS;
        int TIDYING    =  2 << COUNT_BITS;
        int TERMINATED =  3 << COUNT_BITS;

        System.out.println("Integer.toBinaryString(CAPACITY) = " + Integer.toBinaryString(CAPACITY)); //000_11111111111111111111111111111
        System.out.println("Integer.toBinaryString(RUNNING) = " + Integer.toBinaryString(RUNNING)); // 111_00000000000000000000000000000
        System.out.println("Integer.toBinaryString(SHUTDOWN) = " + Integer.toBinaryString(SHUTDOWN)); // 000_00000000000000000000000000000
        System.out.println("Integer.toBinaryString(STOP) = " + Integer.toBinaryString(STOP)); // 001_00000000000000000000000000000
        System.out.println("Integer.toBinaryString(TIDYING) = " + Integer.toBinaryString(TIDYING)); // 010_00000000000000000000000000000
        System.out.println("Integer.toBinaryString(TERMINATED) = " + Integer.toBinaryString(TERMINATED)); // 011_00000000000000000000000000000
    }
}
