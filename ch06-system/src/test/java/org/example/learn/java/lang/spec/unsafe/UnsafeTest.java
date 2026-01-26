package org.example.learn.java.lang.spec.unsafe;

import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class UnsafeTest {

    /**
     * This only works for bootstrap-loaded classes.
     * Application code must use reflection.
     */
    @Test(expected = SecurityException.class)
    public void test01() {
        // 用户代码是不能直接使用如下语句的
        Unsafe unsafe = Unsafe.getUnsafe(); // throws SecurityException
    }


    /**
     * 用户只能通过反射获取到Unsafe对象
     */
    @Test
    public void test02() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 用户通过Unsafe对象可以获取不可访问的属性
     */
    @Test
    public void test11() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);

            // 先获取属性的偏移量
            final long markOffset = unsafe.objectFieldOffset(Buffer.class.getDeclaredField("mark"));
            System.out.println("markOffset = " + markOffset);
            // 获取某个对象的该属性
            ByteBuffer byteBuffer = ByteBuffer.allocate(16);
            int markValue = unsafe.getInt(byteBuffer, markOffset);
            System.out.println("markValue = " + markValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
