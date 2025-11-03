package org.example.learn.java.lang.spec.math;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * BigInteger可以表示工程上的"无穷大/小"的整数
 * BigDecimal可以表示工程上的"无穷大/小"的浮点数
 *
 *
 * BigInteger 把“无限长”的二进制补码整数封装成对象，所有运算行为与 Java 原生整数完全一致，只是永远不会溢出。
 *
 *
 *
 *
 *
 * BigInteger的数据通过如下2个字段来保存的
 * int signum  保存数据的正负零标识
 * int[] mag   存储32进制下的数据值,且是big-endian byte-order的存储方式
 *      由于数组长度可以达到Integer.MAX_VALUE,在2^32进制的加持下,可以做到工程意义上的无穷大
 */
public class BigIntegerTest {

    @Test(expected = NumberFormatException.class)
    public void test01() {
        // BigInteger不支持科学计数法
        BigInteger bigInteger = new BigInteger("1.23e5");
        System.out.println("bigInteger = " + bigInteger);
    }

    @Test(expected = Test.None.class)
    public void test02() {
        // BigInteger支持正/负号(默认是十进制的)
        BigInteger bigInteger = new BigInteger("-123000");
        System.out.println("bigInteger = " + bigInteger);
        BigInteger bigInteger2 = new BigInteger("+123000");
        System.out.println("bigInteger2 = " + bigInteger2);

        // 支持leading-zeros
        BigInteger bigInteger3 = new BigInteger("+000123000");
        System.out.println("bigInteger3 = " + bigInteger3);
        BigInteger bigInteger4 = new BigInteger("-000123000");
        System.out.println("bigInteger4 = " + bigInteger4);

        // 支持leading-zeros(特殊场景,只有零)
        BigInteger bigInteger5 = new BigInteger("+000");
        System.out.println("bigInteger5 = " + bigInteger5);
    }

    /**
     * BigInteger(String, int) 不仅支持正/负号,还支持digit是其他进制的(2~36)
     *
     * 正数的补码等于源码,负数的补码等于反码(原码按位取反)+1
     *
     * (-x) 的补码 = ~x + 1
     * x 的补码 = ~( -x 的补码 ) + 1
     * 即“求相反数的补码” = “对所有位取反再加 1”
     * 两个正数相减,等于正数补码(原码)加负数补码
     */
    @Test(expected = Test.None.class)
    public void test03() {
        System.out.println("Integer.parseInt(\"1011100110\", 2) = " + Integer.parseInt("1011100110", 2));

        // 支持正负号/leading-zeros/其他进制的digit
        BigInteger bigInteger1 = new BigInteger( "01011100110", 2); // 二进制构建大整数
        System.out.println("bigInteger1 = " + bigInteger1 + " | bigInteger1.toString(2) = " + bigInteger1.toString(2));

        // BigInteger将数值的正负号和绝对值分开对待的
        Assert.assertEquals("BigInteger为正数时,toString符合常识,返回的是原码", Integer.toBinaryString(bigInteger1.intValue()), bigInteger1.toString(2));
        Assert.assertNotEquals("⚡BigInteger将数值的正负号和绝对值分看对待的.所以负数的toString的第一个符号是负号,后面是绝对值的原码,而非负数的补码.", Integer.toBinaryString(-742), new BigInteger( "-742").toString(2));

        // 由于BigInteger将数值的正负号和绝对值分看对待的,-742用-01011100110表示,其中01011100110是742的二进制,而负数非补码
        BigInteger bigInteger7 = new BigInteger("-01011100110", 2);
        System.out.println("bigInteger7 = " + bigInteger7 + " | bigInteger7.toString(2) = " + bigInteger7.toString(2));
        Assert.assertEquals("BigInteger的toString中使用了负号,所以后面是是绝对值的原码", bigInteger7.abs().toString(2), bigInteger7.toString(2).replace("-", ""));
    }

    /**
     * 支持类似于primitive整数的位运算
     *
     */
    @Test
    public void test04() {
        BigInteger x = new BigInteger( "01011100110", 2); // 二进制构建大整数
        BigInteger y = new BigInteger("-00001001001", 2);
        System.out.println("x = " + x + " | binary presentation is " + Integer.toBinaryString(x.intValue()));
        System.out.println("y = " + y + " | binary presentation is " + Integer.toBinaryString(y.intValue()));

        // 由于当前x/y的值都在int范围内
        System.out.printf("x & y = %s\n", Integer.toBinaryString(x.and(y).intValue()));
        System.out.printf("x | y = %s\n", Integer.toBinaryString(x.or(y).intValue()));
        System.out.printf("x ^ y = %s\n", Integer.toBinaryString(x.xor(y).intValue()));
        System.out.printf("!x = %s\n", Integer.toBinaryString(x.not().intValue()));
        System.out.printf("x & !y = %s\n", Integer.toBinaryString(x.andNot(y).intValue()));
    }

    /**
     * java 语言中,没有指数运算符.
     * 可以使用库方法Math.pow(a, b)
     * ^运算符是位与运算符
     */
    @Test
    public void test05() {
        // (2^32)^1的一种写法
        long r = (long) Math.pow(Math.pow(2, 32), 1);
        System.out.println("r = " + r);

        // (2^32)^1的另一种写法
        BigInteger bigInteger = BigInteger.valueOf(2).pow(32).pow(1);

        // 确保bigInteger小于long的最大值,longValue()才不会发生截断
        Assert.assertTrue(bigInteger.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) < 0);
        Assert.assertEquals(bigInteger.longValue(), r);
    }


    @Test
    public void test10() {
        // mag为 [287, 1912276171]
        BigInteger bigInteger = new BigInteger("1234567890123");


        // 基数为2^32
        BigInteger base = BigInteger.valueOf(2).pow(32);
        // 指数为0
        BigInteger pow0 = base.pow(0);
        // 指数为1
        BigInteger pow1 = base.pow(1);

        // 因为是big-endian的存储方式,所以数组的第一个元素的指数是1,第二个元素的指数是0
        BigInteger part0 = BigInteger.valueOf(287).multiply(pow1);
        BigInteger part1 = BigInteger.valueOf(1912276171).multiply(pow0);


        Assert.assertEquals(part0.add(part1), bigInteger);
    }

    @Test
    public void test11() {
        // mag为 [287, 1912276171]
        BigInteger bigInteger = new BigInteger("1234567890123");
        BigInteger base = BigInteger.valueOf(2).pow(32);
        System.out.println("bigInteger.divide(base) = " + bigInteger.divide(base));
        System.out.println("bigInteger.mod(base.multiply(bigInteger.divide(base))) = " + bigInteger.mod(base.multiply(bigInteger.divide(base))));
    }

    @Test
    public void test21() {
        // mag为 [287, 1912276171]
        BigInteger bigInteger = new BigInteger("1234567890123");
        byte[] byteArray = bigInteger.toByteArray();
        System.out.println("byteArray.length = " + byteArray.length);
        for (byte b : byteArray) {
            System.out.println("Integer.toBinaryString(b) = " + Integer.toBinaryString(b & 0xff));
        }
        System.out.println("------------------------------------");

        System.out.println("bigInteger.bitLength() = " + bigInteger.bitLength());
        System.out.println("Integer.toBinaryString(287) = " + Integer.toBinaryString(287));
        System.out.println("Integer.toBinaryString(1912276171) = " + Integer.toBinaryString(1912276171));

        System.out.println("--------------打印各个部分----------------------");
        int part0Len = bigInteger.bitLength() % (4 * 8);
        System.out.println("part0Len = " + part0Len);
    }
}
