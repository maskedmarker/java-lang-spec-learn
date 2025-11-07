package org.example.learn.java.lang.spec.math;

import org.junit.Test;

/**
 * Java中,数值的字面量声明可以使用指数的写法
 *
 * 以10为底的指数
 * A.BCpN   ≡   (A.BC in base 10) × 10^N
 * 这里的A/B/C和N都是10进制
 *
 * 十六进制浮点常量 以 2 为底的指数
 * 0xA.BCpN   ≡   (A.BC in base 16) × 2^N
 * 这里的A/B/C这里都是16进制, 但是N是10进制
 */
public class ExponentTest {


    @Test
    public void test00() {
        // base是10
        float a = 1.23e5F; // 1.23* (10^5)
        System.out.println("a = " + a);

        double b = 1.4E6; // 1.4* (10^6)
        System.out.println("b = " + b);


        // base是2
        float c = 0x1.23p5F;// (1+2*16^-1+3*16^-2)*(2^5)
        System.out.println("c = " + c);
        double d = 0x1.23P6; // 1.2*(2^6)
        System.out.println("d = " + d);
        double e = 0x1.4P10; // (1+4*16^-1)*(2^10)
        System.out.println("e = " + e);
    }
}
