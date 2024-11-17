package org.example.learn.java.lang.spec.system;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * 测试.class文件的java版本
 *
 * A class file consists of a stream of 8-bit bytes. 16-bit and 32-bit quantities are constructed by reading in two and four consecutive 8-bit bytes, respectively.
 * Multibyte data items are always stored in big-endian order, where the high bytes come first.
 *
 * Java ClassFile versions (the minor version is stored in the 16 most significant bits, and the major version in the 16 least significant bits).
 */
public class JavaVersionTest {


    @Test
    public void test0() throws IOException {
        // 获取Java版本
        String javaVersion = System.getProperty("java.version");
        // 获取Java运行时环境的版本
        String javaRuntimeVersion = System.getProperty("java.runtime.version");

        System.out.println("javaVersion = " + javaVersion);
        System.out.println("javaRuntimeVersion = " + javaRuntimeVersion);
    }

    @Test
    public void test1() throws IOException {
        InputStream resourceAsStream = Object.class.getResourceAsStream(Object.class.getSimpleName() + ".class");
        Assert.assertNotNull(resourceAsStream);

        // Check the class' major_version. This field is after the magic and minor_version fields, which use 4 and 2 bytes respectively.
        byte[] bytesMagic = new byte[4];
        byte[] bytesMinorVersion = new byte[2];
        byte[] bytesMajorVersion = new byte[2];
        resourceAsStream.read(bytesMagic);
        resourceAsStream.read(bytesMinorVersion);
        resourceAsStream.read(bytesMajorVersion);

        short minor = readShort(bytesMinorVersion);
        short major = readShort(bytesMajorVersion);;
        System.out.println("minor = " + minor);
        System.out.println("major = " + major);

        // .class files start with the "magic number" 0xCAFEBABE, which is stored in big-endian order.
        System.out.println(Integer.toHexString((bytesMagic[0] << 24) & 0xFF000000));
        System.out.println(Integer.toHexString((bytesMagic[1] << 16) & 0xFF0000));
        System.out.println(Integer.toHexString((bytesMagic[2] << 8) & 0xFF00));
        System.out.println(Integer.toHexString(bytesMagic[3] & 0xFF));



        System.out.println("magic code = " +
                String.format("%02x %02x %02x %02x",
                        bytesMagic[0] & 0xFF, bytesMagic[1] & 0xFF, bytesMagic[2] & 0xFF, bytesMagic[3] & 0xFF));
    }

    public short readShort(byte[] data) {
        // .class文件是big-endian
        return (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
    }
}
