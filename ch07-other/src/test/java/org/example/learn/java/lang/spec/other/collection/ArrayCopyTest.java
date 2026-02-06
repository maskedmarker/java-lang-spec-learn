package org.example.learn.java.lang.spec.other.collection;

import org.junit.Test;

import java.util.Arrays;

/**
 * 数组复制


 System.arraycopy

 在Linux 平台上,java.lang.System.arraycopy 最终会落到HotSpot JVM内部的 arraycopy Stub,
 对于primitive数组,本质上就是一次高度优化的 memmove/memcpy; 对于 reference 数组,则是带 GC write barrier 的逐元素拷贝.
 memcpy不是简单地直接调用glibc的memcpy而是：JVM 自己决定用 intrinsic / 汇编 stub / C++ runtime / memmove / 向量化 copy / GC-aware copy 中的哪一种


 对比 Unsafe.copyMemory vs System.arraycopy
 System.arraycopy 是“语义完整 + GC 安全 + JVM 强保证”的拷贝; Unsafe.copyMemory 是“裸内存搬运指令”,速度潜力更大,但责任完全在调用者.
 System.arraycopy = JVM托管的memmove; Unsafe.copyMemory =你自己写的memcpy(我只帮你把这段字节挪过去,其他一概不管.)
 Unsafe.copyMemory的主战场是off-heap/DirectMemory

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
     * src/dest是同一个对象 (支持判断是否有内存重叠overlap)
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
