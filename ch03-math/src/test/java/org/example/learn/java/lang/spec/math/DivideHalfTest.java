package org.example.learn.java.lang.spec.math;

import org.junit.Test;

/**
 * 平均二分
 *
 *  low和hi为整数int或long  (low + hi) >>> 1 可以精确的计算中中位数
 *  (low + hi)的溢出不会影响计算结果
 */
public class DivideHalfTest {

    /**
     * 偶数个,可以正好平分
     */
    @Test
    public void test01() {
        int low = 3, hi = 10;
        int middle = (low + hi) >>> 1;  // [3, 6]  [7, 10]
        System.out.println("middle = " + middle);
    }
    /**
     * 奇数个,无法正好平分
     *      左侧比右侧多一个
     */
    @Test
    public void test02() {
        int low = 3, hi = 7;
        int middle = (low + hi) >>> 1;  // [3, 5]  [6, 7]  左侧比右侧多一个
        System.out.println("middle = " + middle);
    }

    /**
     * 在位移前相加溢出(刚溢出),并不影响计算
     */
    @Test
    public void test11() {
        int low = 1, hi = Integer.MAX_VALUE;
        int middle = (low + hi) >>> 1;
        System.out.println("middle = " + middle);

        System.out.println("Integer.toBinaryString((low + hi)) = " + Integer.toBinaryString((low + hi)));
        System.out.println("Integer.toBinaryString((low + hi) >>> 1) = " + Integer.toBinaryString((low + hi) >>> 1));

        // 使用long是为了不溢出,便于计算偏差
        long lowL = low, hiL = hi;
        long offset = Math.abs(((lowL + hiL) >>> 1) - middle);
        System.out.println("offset = " + offset);
    }

    /**
     * 在位移前相加溢出(大量溢出),并不影响计算
     */
    @Test
    public void test12() {
        int low = Integer.MAX_VALUE, hi = Integer.MAX_VALUE;
        int middle = (low + hi) >>> 1;
        System.out.println("middle = " + middle);

        System.out.println("Integer.toBinaryString((low + hi)) = " + Integer.toBinaryString((low + hi)));
        System.out.println("Integer.toBinaryString((low + hi) >>> 1) = " + Integer.toBinaryString((low + hi) >>> 1));

        // 使用long是为了不溢出,便于计算偏差
        long lowL = low, hiL = hi;
        long offset = Math.abs(((lowL + hiL) >>> 1) - middle);
        System.out.println("offset = " + offset);
    }
}
