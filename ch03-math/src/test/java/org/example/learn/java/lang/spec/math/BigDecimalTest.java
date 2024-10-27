package org.example.learn.java.lang.spec.math;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class BigDecimalTest {


    @Test
    public void test0() {
        // BigDecimal是有BigInteger和scale构成得到
        BigDecimal a = new BigDecimal("5.4");
    }

    @Test
    public void test1() {
        // 商和余数
        BigDecimal a = new BigDecimal("12.345");
        BigDecimal b = new BigDecimal("0.12");
        BigDecimal[] dr = a.divideAndRemainder(b);
        System.out.println(dr[0]); // 102
        System.out.println(dr[1]); // 0.105
    }

    @Test
    public void test2() {
        // 商和余数
        BigDecimal a = new BigDecimal("5.4");
        BigDecimal b = new BigDecimal("0.12");
        BigDecimal[] dr = a.divideAndRemainder(b);
        System.out.println(dr[0]); // 102
        System.out.println(dr[1]); // 0.105

        // 根据余数来判断是否整除
        if (dr[1].signum() == 0) {
            System.out.println(String.format("%s是%s的整数倍", a.toPlainString(), b.toPlainString()));
        }
    }

    /**
     * 平均分配,尾差给最后一位
     */
    @Test
    public void test3() {
        BigDecimal total = new BigDecimal("5.5");
        // 没人实际获得
        BigDecimal[] earns = new BigDecimal[3];
        // 平均值
        BigDecimal avg = total.divide(new BigDecimal(earns.length), 2, BigDecimal.ROUND_DOWN);
        System.out.println("avg = " + avg);

        // 用平均值赋值
//        Arrays.stream(earns).forEach(i -> i = new BigDecimal(avg.toPlainString())); // stream的元素immutable
        for (int i = 0; i < earns.length; i++) {
            earns[i] = new BigDecimal(avg.toPlainString());
        }

        // 尾差
        BigDecimal alpha = total.subtract(Arrays.stream(earns).reduce(BigDecimal.ZERO, BigDecimal::add));
        System.out.println("alpha = " + alpha);

        earns[earns.length - 1] = earns[earns.length - 1].add(alpha);

        Arrays.stream(earns).forEach(System.out::println);
    }
}
