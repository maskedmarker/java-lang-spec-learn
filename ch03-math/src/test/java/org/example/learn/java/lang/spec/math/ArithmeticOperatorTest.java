package org.example.learn.java.lang.spec.math;

import org.junit.Test;

/**
 * 算数运算符
 *
 * 加法 addition
 * 减法 subtraction
 * 乘法 multiplication
 * 除法 division
 *      被除数/除数/商数/余数
 *      dividend/divisor/quotient/remainder
 *
 */
public class ArithmeticOperatorTest {

    /**
     * 除号运算(整型)
     *
     * 商和余数都是整型的
     */
    @Test
    public void testDivisionInteger() {
        int dividend = 10;
        int divisor = 3;

        int quotient = dividend / divisor;
        int remainder = dividend % divisor;
        System.out.println("quotient = " + quotient);
        System.out.println("remainder = " + remainder);
    }

    /**
     * 除号运算(浮点型)
     *
     * 商和余数都是浮点型的
     */
    @Test
    public void testDivisionFloat() {
        float dividend = 10.0F;
        float divisor = 3.0F;

        float quotient = dividend / divisor;
        float remainder = dividend % divisor;
        System.out.println("quotient = " + quotient);
        System.out.println("remainder = " + remainder);

        System.out.println("---------------------------------");
        divisor = 3.3F;
        quotient = dividend / divisor;
        remainder = dividend % divisor;
        System.out.println("quotient = " + quotient);
        System.out.println("remainder = " + remainder);
    }
}
