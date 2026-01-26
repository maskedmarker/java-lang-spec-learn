package org.example.learn.java.lang.spec.io.net.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BufferUtils {

    private static Unsafe UNSAFE;
    private static final long markOffset;
    private static final long hbOffset;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

            markOffset = UNSAFE.objectFieldOffset(Buffer.class.getDeclaredField("mark"));
            hbOffset = UNSAFE.objectFieldOffset(ByteBuffer.class.getDeclaredField("hb"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static int getMarkValue(Buffer buffer) {
        return UNSAFE.getInt(buffer, markOffset);
    }

    public static byte[] getHbValue(ByteBuffer buffer) {
        return (byte[]) UNSAFE.getObject(buffer, hbOffset);
    }

    /**
     * capacity是底层数组的长度,即buffer的最多可以容纳多少字节(初始化后不能改动)
     * A buffer's limit is the index of the first element that should not be read or written.
     * A buffer's position is the index of the next element to be read or written.
     *
     * remaining在读/写模式下的含义不同.
     *      读模式下,remaining表示还可以读取多少个字节
     *      写模式下,remaining表示还可以写入多少个字节
     *
     * position指向下一个要读取的字节的index,或者下一个要写入的字节的index
     *      rewind()可以将其置零
     *      mark()可以临时标记某一时刻的position,然后通过reset()将position重新设置到该位置
     *      compact()也会改变position,使position移动到下一个待操作的index上
     *
     *
     */
    public static void logBufferStat(String info, ByteBuffer buffer) {
        System.out.printf("--%s--\n", info);
        System.out.printf("position: %d, limit: %d, capacity: %d, remaining: %d, mark: %d, hb: %s\n", buffer.position(), buffer.limit(), buffer.capacity(), buffer.remaining(), BufferUtils.getMarkValue(buffer), Arrays.toString(BufferUtils.getHbValue(buffer)));
    }
}
