package org.example.learn.java.lang.spec.other.operator;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * 比较运算符(comparison operator)有
 * > == < >= <= !=
 *
 *
 * 比较运算符构成的比较表达式的值是逻辑true或false
 */
public class ComparisonOperatorTest {


    /**
     * 比较运算符
     */
    @Test
    public void test01() {
        if (3 > 2) {
            System.out.println("大于运算符  属于 比较运算符");
        }

        if (3 == 2) {
            System.out.println("等于运算符  属于 比较运算符");
        }

        if (3 < 4) {
            System.out.println("小于运算符  属于 比较运算符");
        }

        if (3 >= 2) {
            System.out.println("大于等于运算符  属于 比较运算符");
        }

        if (3 <= 4) {
            System.out.println("小于等于运算符  属于 比较运算符");
        }

        if (3 != 4) {
            System.out.println("不等于运算符  属于 比较运算符");
        }
    }

    /**
     * 比较表达式
     */
    @Test
    public void test02() {
        Assert.assertEquals(true, (3 > 2));

        System.out.println("比较表达式的值是逻辑true或false");
    }
}
