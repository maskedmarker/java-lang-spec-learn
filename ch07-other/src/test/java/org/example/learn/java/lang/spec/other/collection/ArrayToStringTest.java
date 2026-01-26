package org.example.learn.java.lang.spec.other.collection;

import org.junit.Test;

import java.util.Arrays;

/**
 * 将数组转换为可读的字符串
 * array.toString()的输出结果是type+identity_hash,不具有可读性
 */
public class ArrayToStringTest {

    // region jdk自带的工具

    /**
     * Arrays.toString适用于一维数组,不适合多维数组
     * Arrays.deepToString适合多维数组,不适用于一维数组
     */
    @Test
    public void test01() {
        System.out.println("Arrays.toString(null) = " + Arrays.toString((int[])null));

        int[] a = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        System.out.println("Arrays.toString(a) = " + Arrays.toString(a));


        int[][] aa = {{1, 2}, {3, 4}};
        System.out.println("Arrays.deepToString(aa) = " + Arrays.deepToString(aa));

        int[][][] aaa = {{{111, 112}, {121, 122}}, {{211, 212}, {221, 222}}};
        System.out.println("Arrays.deepToString(aaa) = " + Arrays.deepToString(aaa));
    }

    @Test
    public void test02() {
        int[] a = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        System.out.println("toArrayString(a) = " + toArrayString(a, ",", "[", "]"));
    }

    private String toArrayString(int[] arr, String delimiter, String prefix, String suffix) {
        if (arr == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) {
                sb.append(delimiter);
            }
        }
        sb.append(suffix);

        return sb.toString();
    }
}
