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
        System.out.println("Integer.toBinaryString(i) = " + Integer.toBinaryString(i));
        System.out.println("Integer.bitCount(i) = " + Integer.bitCount(i));
        Assert.assertEquals("Integer.bitCount()计算的时二进制中1的个数,不包含0", 3, Integer.bitCount(i));
    }

    @Test
    public void test1() {
        int i = 0b11010;
        BigInteger bigInteger = BigInteger.valueOf(i);
        System.out.println("bigInteger.bitCount() = " + bigInteger.bitCount());
        System.out.println("bigInteger.bitLength() = " + bigInteger.bitLength());

        Assert.assertEquals("BigInteger.bitCount()计算的时二进制中1的个数,不包含0", 3, bigInteger.bitCount());
        Assert.assertEquals("BigInteger.bitLength()计算的时二进制的长度的个数,不包含leading-zero,但是包含中间和结尾的zero", 5, bigInteger.bitLength());
    }
}
