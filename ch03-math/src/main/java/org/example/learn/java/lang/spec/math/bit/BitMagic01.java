package org.example.learn.java.lang.spec.math.bit;

public class BitMagic01 {
    /**
     * 判断正整数n是否是2的幂
     *    如果n是2的幂,
     *          n的补码中只有最高位的是1,其低位都是0
     *          (n-1)的补码中只有1,且都低于n的最高位1
     *          此时n与(n-1)进行位的与运算,结果为0
     */
    public boolean isPowerOfTwo(int n) {
        if (n < 0) {
            throw new RuntimeException("不支持负数");
        }

        return (n & (n-1)) == 0;
    }

}
