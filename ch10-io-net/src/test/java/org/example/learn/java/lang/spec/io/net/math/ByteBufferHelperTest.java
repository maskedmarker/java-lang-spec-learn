package org.example.learn.java.lang.spec.io.net.math;

import org.junit.Test;


public class ByteBufferHelperTest {


    /**
     * 大端序：高位字节在前（网络字节序）
     * int: 0x12345678 -> bytes: 0x12, 0x34, 0x56, 0x78
     */
    @Test
    public void test0() {
        byte[] bytes1 = ByteBufferHelper.bytesBigEndian(0x12345678);
        System.out.println("ByteBufferHelper.toHexString(bytes1) = " + ByteBufferHelper.toHexString(bytes1));

        byte[] bytes2 = ByteBufferHelper.bytesBigEndian2(0x12345678);
        System.out.println("ByteBufferHelper.toHexString(bytes2) = " + ByteBufferHelper.toHexString(bytes2));

        byte[] bytes3 = ByteBufferHelper.intsToBytesBE(0x12345678);
        System.out.println("ByteBufferHelper.toHexString(bytes3) = " + ByteBufferHelper.toHexString(bytes3));
    }

    /**
     * 小端序：低位字节在前（x86架构常用）
     * int: 0x12345678 -> bytes: 0x78, 0x56, 0x34, 0x12
     */
    @Test
    public void test1() {
        byte[] bytes1 = ByteBufferHelper.bytesLittleEndian(0x12345678);
        System.out.println("ByteBufferHelper.toHexString(bytes1) = " + ByteBufferHelper.toHexString(bytes1));

        byte[] bytes2 = ByteBufferHelper.bytesLittleEndian2(0x12345678);
        System.out.println("ByteBufferHelper.toHexString(bytes2) = " + ByteBufferHelper.toHexString(bytes2));
    }
}
