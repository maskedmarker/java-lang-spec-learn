package org.example.learn.java.lang.spec.math.bit;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public class IntegerTest {

    /**
     * bitCount计算的时二进制中1的个数,不包含0
     */
    @Test
    public void test0() {
        int i = 0b11010;
        System.out.println("i = " + i);
        System.out.println("Integer.toBinaryString(i) = " + Integer.toBinaryString(i));
        System.out.println("Integer.bitCount(i) = " + Integer.bitCount(i));
        Assert.assertEquals("Integer.bitCount()计算的时二进制中1的个数,不包含0", 3, Integer.bitCount(i));
    }

    @Test
    public void test1() {
        int i = 0b11010;
        BigInteger bigInteger = BigInteger.valueOf(i);
        System.out.println("bigInteger = " + bigInteger);
        System.out.println("bigInteger.bitCount() = " + bigInteger.bitCount());
        System.out.println("bigInteger.bitLength() = " + bigInteger.bitLength());

        Assert.assertEquals("当为正数时, BigInteger.bitCount()计算的是二进制中1的个数,不包含0", 3, bigInteger.bitCount());
        Assert.assertEquals("当为正数时, BigInteger.bitLength()计算的是二进制的长度的个数,不包含leading-zero,但是包含中间和结尾的zero", 5, bigInteger.bitLength());
    }

    @Test
    public void test10() {
        int j = 0b10000000_00000000_00000000_00000001;
        System.out.println("j = " + j);
        System.out.println("Integer.toBinaryString(j) = " + Integer.toBinaryString(j));
        System.out.println("Integer.bitCount(j) = " + Integer.bitCount(j));
        Assert.assertEquals("Integer.bitCount()计算的时二进制中1的个数,不包含0", 2, Integer.bitCount(j));
    }

    /**
     * BigInteger处理负数时,比较特殊
     * signum存储负号信息
     * mag存储对应的正数信息
     */
    @Test
    public void test11() {
        int j = 0b10000000_00000000_00000000_00000001;
        BigInteger bigInteger = BigInteger.valueOf(j);
        System.out.println("bigInteger = " + bigInteger);
        System.out.println("bigInteger.bitCount() = " + bigInteger.bitCount());
        System.out.println("bigInteger.bitLength() = " + bigInteger.bitLength());

        int i = -j;
        BigInteger bigIntegerI = BigInteger.valueOf(i);
        System.out.println("bigIntegerI = " + bigIntegerI);
        System.out.println("Integer.toBinaryString(i) = " + Integer.toBinaryString(i));
        System.out.println("bigIntegerI.bitCount() = " + bigIntegerI.bitCount());
        System.out.println("bigIntegerI.bitLength() = " + bigIntegerI.bitLength());
        Assert.assertEquals(bigInteger.negate(), bigIntegerI);



        Assert.assertEquals("当为负数时, BigInteger.bitCount()计算的是对应正数的二进制中1的个数,不包含0", 2, bigInteger.bitCount());
        Assert.assertEquals("当为负数时, BigInteger.bitLength()计算的是对应正数的二进制的长度的个数,不包含符号位,不包含leading-zero,但是包含中间和结尾的zero", 31, bigInteger.bitLength());
    }
}
