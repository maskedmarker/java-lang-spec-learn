package org.example.learn.java.lang.spec.io.net.math;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 高位字节在前  大端常用于网络传输, 也被称为 网络字节序
 */
public class ByteBufferHelper {

    /**
     * 将int转换为大端的字节数组
     */
    public static byte[] bytesBigEndian(int... ints) {
        ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4);
        buffer.order(ByteOrder.BIG_ENDIAN); // 默认就是大端序

        for (int value : ints) {
            buffer.putInt(value);
        }

        return buffer.array();
    }

    public static byte[] bytesLittleEndian(int... ints) {
        ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int value : ints) {
            buffer.putInt(value);
        }

        return buffer.array();
    }


    public static byte[] bytesBigEndian2(int... ints) {
        ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4);
        // 默认就是大端序

        // 创建IntBuffer视图(主要是操作方便)
        buffer.asIntBuffer().put(ints);

        return buffer.array();
    }

    public static byte[] bytesLittleEndian2(int... ints) {
        ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 创建IntBuffer视图(主要是操作方便)
        buffer.asIntBuffer().put(ints);

        return buffer.array();
    }

    // --------------------------   手动位移操作   --------------------------

    /**
     * 大端序：高位字节在前（网络字节序）
     * int: 0x12345678 -> bytes: 0x12, 0x34, 0x56, 0x78
     */
    public static byte[] intsToBytesBE(int... ints) {
        byte[] result = new byte[ints.length * 4];

        for (int i = 0; i < ints.length; i++) {
            int value = ints[i];
            int pos = i * 4;

            result[pos]     = (byte) ((value >> 24) & 0xFF); // 最高字节
            result[pos + 1] = (byte) ((value >> 16) & 0xFF);
            result[pos + 2] = (byte) ((value >> 8) & 0xFF);
            result[pos + 3] = (byte) (value & 0xFF);         // 最低字节
        }

        return result;
    }

    /**
     * 小端序：低位字节在前（x86架构常用）
     * int: 0x12345678 -> bytes: 0x78, 0x56, 0x34, 0x12
     */
    public static byte[] intsToBytesLE(int... ints) {
        byte[] result = new byte[ints.length * 4];

        for (int i = 0; i < ints.length; i++) {
            int value = ints[i];
            int pos = i * 4;

            result[pos]     = (byte) (value & 0xFF);         // 最低字节
            result[pos + 1] = (byte) ((value >> 8) & 0xFF);
            result[pos + 2] = (byte) ((value >> 16) & 0xFF);
            result[pos + 3] = (byte) ((value >> 24) & 0xFF); // 最高字节
        }

        return result;
    }

    // --------------------------   逆操作：byte数组转回int   --------------------------
    public static int[] bytesToIntsBE(byte[] bytes) {
        if (bytes.length % 4 != 0) {
            throw new IllegalArgumentException("字节数组长度必须是4的倍数");
        }

        int[] result = new int[bytes.length / 4];

        for (int i = 0; i < result.length; i++) {
            int pos = i * 4;
            result[i] = ((bytes[pos] & 0xFF) << 24) |
                    ((bytes[pos + 1] & 0xFF) << 16) |
                    ((bytes[pos + 2] & 0xFF) << 8) |
                    (bytes[pos + 3] & 0xFF);
        }

        return result;
    }


    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
