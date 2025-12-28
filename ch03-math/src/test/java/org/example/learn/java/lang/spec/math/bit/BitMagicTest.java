package org.example.learn.java.lang.spec.math.bit;

import org.junit.Test;

/**
 * 通过位运算的魔法方法
 */
public class BitMagicTest {

    /**
     * 计算大于n的最小2的幂
     *      将最高位1及其低位都填充成1,然后再加一,通过进位得到,比最高位1更高一位是1,其低位都是0,即可获取大于n的最小2的幂
     */
    @Test
    public void test01() {
        int t;

        for (int i = 0b1000; i < 0b10000; i++) {
            t = BitMagic02.SmallestPowerOfTwoMoreThan(i);
            System.out.printf("%s -> %s\n", Integer.toBinaryString(i), Integer.toBinaryString(t));
        }

        System.out.printf("%s -> %s\n", Integer.toBinaryString(0b10001), Integer.toBinaryString(BitMagic02.SmallestPowerOfTwoMoreThan(0b10001)));
    }

    /**
     * 计算大于等于n的最小2的幂
     */
    @Test
    public void test02() {
        int t;

        for (int i = 0b1000; i < 0b10000; i++) {
            t = BitMagic02.SmallestPowerOfTwoNoLessThan(i);
            System.out.printf("%s -> %s\n", Integer.toBinaryString(i), Integer.toBinaryString(t));
        }

        System.out.printf("%s -> %s\n", Integer.toBinaryString(0b10001), Integer.toBinaryString(BitMagic02.SmallestPowerOfTwoNoLessThan(0b10001)));
    }

    /**
     * 计算小于等于n的最大2的幂
     *      只保留最高位1,其低位都填充成0,即可获取小于等于n的最大2的幂
     */
    @Test
    public void test03() {
        int t;

        for (int i = 0b1000; i < 0b10000; i++) {
            t = BitMagic02.maxPowerOfTwoNoMoreThan(i);
            System.out.printf("%s -> %s\n", Integer.toBinaryString(i), Integer.toBinaryString(t));
        }

        System.out.printf("%s -> %s\n", Integer.toBinaryString(0b10001), Integer.toBinaryString(BitMagic02.maxPowerOfTwoNoMoreThan(0b10001)));
    }
}
