package org.example.learn.java.lang.spec.math.bit;

import org.junit.Assert;
import org.junit.Test;

/**
 * & | ^ 在不同类型的操作数下,表达的意思是不同的
 * 操作数为整数类型(byte/short/int/long)时, 位与运算符(&)/位或运算符(|)/位异或运算符(^)
 * 操作数为bool类型时, 逻辑与运算符(&)/逻辑或运算符(|)/逻辑异或运算符(^)
 */
public class BitOperatorTest {

    /**
     * 基础的位运算符操作
     */
    @Test
    public void test0() {
        // 位与运算符(&)
        Assert.assertEquals("位与运算符,operand的每个bit取与运算", 0b0000, (0b0001 & 0b0010));

        // 位或运算符(|)
        Assert.assertEquals("位或运算符,operand的每个bit取或运算", 0b0011, (0b0001 | 0b0010));
        Assert.assertEquals("位或运算符,operand的每个bit取或运算", 0b1011, (0b1001 | 0b1010));

        // 位异或运算符(^)
        Assert.assertEquals("位或运算符,operand的每个bit取异或运算", 0b0011, (0b0001 ^ 0b0010));
        Assert.assertEquals("位或运算符,operand的每个bit取异或运算", 0b0011, (0b1001 ^ 0b1010));


        System.out.println("--------------------------为了较少迷惑, 下面是相同符号的逻辑运算符--------------------------");
        // 逻辑与运算符(&)
        Assert.assertEquals("逻辑与运算符,operand必须都是true,结果才是true", true, true & true);
        Assert.assertEquals("逻辑与运算符,operand有一个是false,结果就是false", false, true & false);
        Assert.assertEquals("逻辑与运算符,operand有一个是false,结果就是false", false, false & true);

        // 逻辑或运算符(|)
        Assert.assertEquals("逻辑或运算符,operand有一个是true,结果就是true", true, true | false);
        Assert.assertEquals("逻辑或运算符,operand有一个是true,结果就是true", true, false | true);
        Assert.assertEquals("逻辑或运算符,operand都是false,结果才是false", false, false | false);

        // 逻辑异或运算符(^)
        Assert.assertEquals("逻辑异或运算符,operand不相同时,结果就是true", true, true | false);
        Assert.assertEquals("逻辑异或运算符,operand不相同时,结果就是true", true, false | true);
        Assert.assertEquals("逻辑异或运算符,operand相同时,结果就是false", false, false ^ false);
        Assert.assertEquals("逻辑异或运算符,operand相同时,结果就是false", false, true ^ true);
    }

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
