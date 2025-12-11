package org.example.learn.java.lang.spec.math.arithmetic;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数学上通常定义的“模运算”（mod）要求结果 总是非负的
 * 但在 Java 中,% 实际上是 余数运算（remainder）,它的结果符号与被除数（左操作数）相同
 * 即 Java 的 % 运算结果可以是负数
 *
 * / 运算返回商,% 返回余数
 *
 *
 * a = (b * (a / b)) + (a % b)
 * 这个等式在 Java 中始终成立
 */
public class ModOperatorTest {

    /**
     * a / b的值是商(将a按b的大小,切分为n份,n可以为负, n即为商)
     * a % b的值是余数(将a按b的大小,切分为n份,n可以为负,剩余不够一份的即为余数)
     */
    @Test
    public void test00() {
        int a = 10;
        int b = 3;
        System.out.println(a / b); // 3
        System.out.println(a % b); // 1
    }

    @Test
    public void test01() {
        int a = 10;
        int b = -3;
        System.out.printf("%d/%d is %d, %d%%%d is %d\n", a, b, (a / b), a, b, (a % b));
        System.out.printf("%d/%d is %d, %d%%%d is %d\n", a, (-b), (a / -b), a, (-b), (a % -b));

        Assert.assertEquals("java的模运算的余数与右操作数的正负无关,因为商数调整正负就行", (a / b), -(a / (-b)));
        Assert.assertEquals("java的模运算的余数与右操作数的正负无关,因为商数调整正负就行", (a % b), (a % (-b)));
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 当数组的长度是2的幂时,可以通过位与运算来获取数组index
     */
    @Test
    public void test10() {
        int size = (int) Math.pow(2, 4);
        System.out.println("size = " + size);

        int mask = size -1;
        System.out.println("Integer.toBinaryString(mask) = " + Integer.toBinaryString(mask));

        int r = ThreadLocalRandom.current().nextInt(100);
        for (int i = r; i < r+size; i++) {                          // (r+size-1)-r+1==size
            int index = i & mask;
            System.out.printf("i=%d, index=%d (%s)\n", i, index, Integer.toBinaryString(index)); // 每个index的bit序列都不同
        }
    }
}
