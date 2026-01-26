package org.example.learn.java.lang.spec.other.collection;

import org.junit.Test;

import java.util.Arrays;

/**
 * 数组复制
 */
public class ArrayCopyTest {

    /**
     * System.arraycopy是native方法,对于数组的复制是高效的
     * src/dest不是同一个对象
     */
    @Test
    public void test01() {
        int[] src = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] dest = new int[10];

        System.arraycopy(src, 2, dest, 4, 2);
        System.out.println("Arrays.toString(dest) = " + Arrays.toString(dest));
    }

    /**
     * System.arraycopy是native方法,对于数组的复制是高效的
     * src/dest是同一个对象
     *
     * If the src and dest arguments refer to the same array object,
     * then the copying is performed as if the components at positions srcPos through srcPos+length-1 were first copied to a temporary array with length components
     * and then the contents of the temporary array were copied into positions destPos through destPos+length-1 of the destination array.
     */
    @Test
    public void test02() {
        int[] src = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        System.out.println("Arrays.toString(src) = " + Arrays.toString(src));
        System.arraycopy(src, 2, src, 4, 2);
        System.out.println("Arrays.toString(src) = " + Arrays.toString(src));
    }
}
