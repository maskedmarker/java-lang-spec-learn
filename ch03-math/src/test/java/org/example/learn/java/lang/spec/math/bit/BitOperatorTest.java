package org.example.learn.java.lang.spec.math.bit;

import org.junit.Assert;
import org.junit.Test;

/**
 * 位运算符
 */
public class BitOperatorTest {

    /**
     * Integer.highestOneBit返回值的二进制表示中至多一直一个1,其他都是0,且1必须在最左侧
     * Returns an int value with at most a single one-bit, in the position of the highest-order ("leftmost") one-bit in the specified int value.
     * <p>
     * 假定入参为i,返回值为r,则有如下特性
     * r=2^n,且2^n是不大于i的最大值
     */
    @Test
    public void testHighestOneBit() {
        int highestOneBit = Integer.highestOneBit(1024);
        System.out.println("highestOneBit = " + highestOneBit);
        System.out.println("toBinaryString(highestOneBit) = " + Integer.toBinaryString(highestOneBit));

        highestOneBit = Integer.highestOneBit(9);
        System.out.println("highestOneBit = " + highestOneBit);
        System.out.println("toBinaryString(highestOneBit) = " + Integer.toBinaryString(highestOneBit));

        highestOneBit = Integer.highestOneBit(0);
        System.out.println("highestOneBit = " + highestOneBit);
        System.out.println("toBinaryString(highestOneBit) = " + Integer.toBinaryString(highestOneBit));

        highestOneBit = Integer.highestOneBit(Integer.MIN_VALUE);
        System.out.println("highestOneBit = " + highestOneBit);
        System.out.println("toBinaryString(highestOneBit) = " + Integer.toBinaryString(highestOneBit));
    }

    /**
     * ~ 是一个按位取反运算符（bitwise complement operator）
     * 只适用于 整数类型（byte, short, int, long），不能用于浮点数、boolean 或对象类型
     *
     * ~ 将整数的每一个二进制位都反转（0 变 1，1 变 0）
     */
    @Test
    public void testBitwiseComplementOperator() {
        int mask = 0x0000ffff;
        System.out.println("Integer.toHexString(~mask) = " + Integer.toHexString(~mask));
        Assert.assertTrue((~mask) == 0xffff0000);
    }
}
