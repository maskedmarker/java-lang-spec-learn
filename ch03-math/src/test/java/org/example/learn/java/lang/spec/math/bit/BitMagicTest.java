package org.example.learn.java.lang.spec.math.bit;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * 通过位运算的魔法方法
 */
public class BitMagicTest {

    /**
     * 计算大于等于n的最小2的幂
     *      将最高位1及其低位都填充成1,即可获取大于等于n的最小2的幂
     */
    @Test
    public void test01() {
        int n = 7;
        System.out.println("Integer.toBinaryString(n) = " + Integer.toBinaryString(n));

        // 为了便于描述,假设最高位1的是index是i
        int t = n;

        // 将数字n的最高位1及其后面的地位都填充成1,即可获取大于等于n的最小2的幂
        t |= t >>> 1;   // 保证[i, i-1]位填充为1      如果i-1<0时,i=0, [0]位填充为1
        t |= t >>> 2;   // 保证[i, i-3]位填充为1      如果i-3<0时,i=2/1/0, [2,0]/[1,0]/[0]位填充为1
        t |= t >>> 4;   // 保证[i, i-7]位填充为1      如果i-7<0时,i=6/5/4/3/2/1/0, [6,0]/[5,0]/[4,0]/[3,0]/[2,0]/[1,0]/[0]位填充为1
        t |= t >>> 8;   // 保证最[i, i-15]位填充为1   如果i-15<0时,i=15/.../1/0, [15,0]/.../[1,0]/[0]位填充为1
        t |= t >>> 16;  // 保证[i, i-31]位填充为1     如果i-31<0时,i=30/.../1/0, [30,0]/.../[1,0]/[0]位填充为1

        // 此时t最高位1及其后面的低位都是1
        t = t + 1;     // t加一后通过进位,最高位1的更高一位是1,低位都是0  (这就可以获取大于等于n的最小2的幂)

        System.out.println("t = " + t);
    }

    /**
     * 计算小于等于n的最大2的幂
     *      只保留最高位1,其低位都填充成0,即可获取小于等于n的最大2的幂
     */
    @Test
    public void test02() {
        int n = 7;
        System.out.println("Integer.toBinaryString(n) = " + Integer.toBinaryString(n));


        int t = n;
        // 先将最高位1及其后面的地位都填充成1
        t |= t >>> 1; t |= t >>> 2; t |= t >>> 4;
        t |= t >>> 8; t |= t >>> 16;
        // 再将低位1移除,只保留最高位1
        t = t - (t >>>1);

        System.out.println("t = " + t);
    }
}
