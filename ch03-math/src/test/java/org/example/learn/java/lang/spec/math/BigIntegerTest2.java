package org.example.learn.java.lang.spec.math;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * BigInteger内部实现解析
 *
 * bitsPerDigit 用来评估不同进制下的一个digit会最多占用多少bit,这样才方便构造int[] mag长度
 * digitsPerInt 用来约定一个int容纳多少个某进制的digit
 */
public class BigIntegerTest2 {


    /**
     * BigInteger中bitsPerDigit用来储存,不同进制下一个digit最多所占bit数
     *
     * Math.log(i) -> (base e)
     * Math.log10(i) -> (base 10)
     * Math.log(i)/LOG_TWO 等价于log2(i)
     */
    @Test
    public void test00() {
        final double LOG_TWO = Math.log(2.0);
        // 因为取值时的用法是bitsPerDigit[radix],所以数组bitsPerDigit最大索引值为Character.MAX_RADIX,数组bitsPerDigit的长度是Character.MAX_RADIX+1
        final int[] bitsPerDigit = new int[Character.MAX_RADIX + 1];

        System.out.println("不同进制下,一个digit最多占用多少个bit");
        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            // 不同进制下,估算一个digit所占用的bit数为ceil(log2(i)),ceil会导致估算值不会偏小,这样才安全
            // 通过放大1024(即 2^10)倍来保留精度,然后存入int,当使用时就通过位运算右移10位来还原.1024只能保留小数点后3为精度,剩余的部分小数舍弃并向上取1
            // 使用场景: numBits = ((numDigits * bitsPerDigit[radix]) >>> 10) + 1; 其中加1为了防止(numDigits * bitsPerDigit[radix])/1024F的浮点数有小数部分被舍弃
            bitsPerDigit[i] = (int)Math.ceil(1024 * (Math.log(i) / LOG_TWO));
            System.out.printf("log2(%d)=%f | ceil(log2(%d))=%d | bitsPerDigit[%d]=%d\n", i, Math.log(i) / LOG_TWO, i, (int)Math.ceil(Math.log(i) / LOG_TWO), i, bitsPerDigit[i]);
        }
    }

    /**
     * digitsPerInt用来评估一个int容纳多少个某进制的digit
     * digitsPerInt用来评估将多少个某进制的digit划分为一组,该组用一个int表示
     *
     * 假设进制为r,可以容纳n个digit(n为正整数),那么n个digit组成最大值的十进制为r^n-1
     * => n <= logr(Integer.MAX_VALUE+1)
     * 为了保险 干脆取n=logr(Integer.MAX_VALUE)
     *
     */
    @Test
    public void test01() {
        // 因为取值时的用法是digitsPerInt[radix],所以数组digitsPerInt最大索引值为Character.MAX_RADIX,数组digitsPerInt的长度是Character.MAX_RADIX+1
        final int[] digitsPerInt = new int[Character.MAX_RADIX + 1];

        System.out.println("字符串使用不同的进制(假设为A进制),在转换为BigInteger内部的2^32进制下,一个int(即一个2^32进制的digit)最多容纳多少个A进制digit");
        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            // logi(Integer.MAX_VALUE)=log2(Integer.MAX_VALUE)/log2(i)
            digitsPerInt[i] = (int) (Math.log(Integer.MAX_VALUE) / Math.log(i));
            System.out.printf("log%d(Integer.MAX_VALUE)=%d \n", i, digitsPerInt[i]);
        }
    }

    /**
     * digitsPerInt用来约定一个int容纳多少个某进制的digit
     * intRadix用来约定在digitsPerInt[radix]下,对应的进位倍数
     *
     * radix进制下,每digitsPerInt[radix]个digit被分为一组,
     * 此时组与组之间的进位倍数为digitsPerInt[radix]个radix相乘,即radix^digitsPerInt[radix]
     *
     * intRadix[radix] = radix ^ digitsPerInt[radix]
     */
    @Test
    public void test02() {
        final int[] digitsPerInt = new int[Character.MAX_RADIX + 1];
        final int[] intRadix = new int[digitsPerInt.length];

        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            digitsPerInt[i] = (int) (Math.log(Integer.MAX_VALUE) / Math.log(i));
            intRadix[i] = (int) Math.pow(i, digitsPerInt[i]);
            System.out.printf("%d进制下, 一组容纳%d个digit, 组与组之间的进位倍数为 %x \n", i, digitsPerInt[i], intRadix[i]);
        }
    }

    @Test
    public void test11() {
        // 因为取值时的用法是digitsPerInt[radix],所以数组digitsPerInt最大索引值为Character.MAX_RADIX,数组digitsPerInt的长度是Character.MAX_RADIX+1
        final int[] digitsPerLong = new int[Character.MAX_RADIX + 1];

        System.out.println("字符串使用不同的进制(假设为A进制),在转换为BigInteger内部的2^32进制下,一个long(即一个2^64进制的digit)最多容纳多少个A进制digit");
        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            // logi(Long.MAX_VALUE)=log2(Long.MAX_VALUE)/log2(i)
            digitsPerLong[i] = (int) (Math.log(Long.MAX_VALUE) / Math.log(i));
            System.out.printf("log%d(Long.MAX_VALUE)=%d \n", i, digitsPerLong[i]);
        }
    }

    @Test
    public void test12() {
        final int[] digitsPerLong = new int[Character.MAX_RADIX + 1];
        final long[] longRadix = new long[digitsPerLong.length];

        for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
            digitsPerLong[i] = (int) (Math.log(Long.MAX_VALUE) / Math.log(i));
            longRadix[i] = (long) Math.pow(i, digitsPerLong[i]);
            if (longRadix[i] >= Long.MAX_VALUE) {
                digitsPerLong[i] = digitsPerLong[i] - 1;
                longRadix[i] = (long) Math.pow(i, digitsPerLong[i]);
            }
            System.out.printf("%d进制下, 一组(long)容纳%d个digit, 组与组之间的进位倍数为 %x \n", i, digitsPerLong[i], longRadix[i]);
        }
    }

    @Test
    public void test21() {
        BigInteger bigInteger = new BigInteger("de0b6b3a7640000", 16);
        System.out.println("bigInteger.toString(10) = " + bigInteger.toString(10));
    }
}
