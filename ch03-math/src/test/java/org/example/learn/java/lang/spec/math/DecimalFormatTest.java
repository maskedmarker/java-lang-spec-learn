package org.example.learn.java.lang.spec.math;

import org.junit.Test;

import java.math.BigDecimal;

public class DecimalFormatTest {


    @Test
    public void test0() {
        BigDecimal a = new BigDecimal("1234567890123456789000000000.000000000123456789");

        // without an exponent field
        System.out.println("a.toPlainString() = " + a.toPlainString());

        // using engineering notation if an exponent is needed
        System.out.println("a.toEngineeringString() = " + a.toEngineeringString());

        // using scientific notation if an exponent is needed
        System.out.println("a.toString() = " + a.toString());
    }

    @Test
    public void test1() {
        BigDecimal a = new BigDecimal("12345678901234567890");
        BigDecimal b = new BigDecimal("0.000000000123456789");

        System.out.println("toString(): " + a.toString());
        System.out.println("toEngineeringString(): " + a.toEngineeringString());
        System.out.println("toPlainString(): " + a.toPlainString());

        System.out.println();

        System.out.println("toString(): " + b.toString());
        System.out.println("toEngineeringString(): " + b.toEngineeringString());
        System.out.println("toPlainString(): " + b.toPlainString());
    }
}
