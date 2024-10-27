package org.example.learn.java.lang.spec.other.format;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Float/BigDecimal的toString
 *
 * Float.toString会使用科学计数法来表示文本内容
 * BigDecimal.toString在必要的时候,也会使用科学计数法表示
 * BigDecimal.toPlainString永远不会使用科学计数法表示
 *
 */
public class BigDecimalFormatTest {

    @Test
    public void test0() {
        float f = 1000000F;
        BigDecimal bigDecimal = new BigDecimal(f);

        System.out.println("f = " + Float.toString(f));
        System.out.println("bigDecimal = " + bigDecimal.toString());

        f = 100000000000F;
        System.out.println("f = " + Float.toString(f));
        // 不用字符串
        bigDecimal = new BigDecimal(100000000000000000000D);
        System.out.println("bigDecimal = " + bigDecimal.toString());

        bigDecimal = new BigDecimal("100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        System.out.println("bigDecimal = " + bigDecimal.toString());

        // 当使用科学计数法法表示的字符串作为入参时,输出也会使用科学计数法
        bigDecimal = new BigDecimal("123E100");
        System.out.println("bigDecimal = " + bigDecimal.toString());
        System.out.println("bigDecimal = " + bigDecimal.toPlainString());

    }
}
