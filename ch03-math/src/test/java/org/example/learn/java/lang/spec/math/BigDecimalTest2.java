package org.example.learn.java.lang.spec.math;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class BigDecimalTest2 {


    /**
     * 向上取整
     */
    @Test
    public void test1() {
        BigDecimal value = new BigDecimal("0.1");
        BigDecimal newValue = value.setScale(0, BigDecimal.ROUND_UP);
        System.out.println("newValue = " + newValue);
    }

    /**
     * 向下取整
     */
    @Test
    public void test2() {
        BigDecimal value = new BigDecimal("0.1");
        BigDecimal newValue = value.setScale(0, BigDecimal.ROUND_DOWN);
        System.out.println("newValue = " + newValue);
    }
}
