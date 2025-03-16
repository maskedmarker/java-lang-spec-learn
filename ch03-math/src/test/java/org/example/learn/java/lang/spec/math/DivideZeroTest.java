package org.example.learn.java.lang.spec.math;

import org.junit.Assert;
import org.junit.Test;

/**
 * int/long都不支持除以0
 * float/double都支持除以0,且结果为无穷大
 */
public class DivideZeroTest {

    @Test(expected = ArithmeticException.class)
    public void test01() {
        int a = 100;
        int b = 0;
        int result = a / b;
        System.out.println("result = " + result);
    }

    @Test(expected = ArithmeticException.class)
    public void test02() {
        long a = 100;
        long b = 0;
        long result = a / b;
        System.out.println("result = " + result);
    }

    @Test(expected = Test.None.class)
    public void test11() {
        float a = 100;
        float b = 0;
        float result = a / b;
        System.out.println("result = " + result);
        Assert.assertTrue(Float.isInfinite(result));
    }

    @Test(expected = Test.None.class)
    public void test12() {
        double a = 100;
        double b = 0;
        double result = a / b;
        System.out.println("result = " + result);
        Assert.assertTrue(Double.isInfinite(result));
    }
}
