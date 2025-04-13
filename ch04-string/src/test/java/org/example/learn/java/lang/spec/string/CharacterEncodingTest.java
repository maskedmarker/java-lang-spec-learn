package org.example.learn.java.lang.spec.string;

import org.example.learn.java.lang.spec.string.util.Bin;
import org.example.learn.java.lang.spec.string.util.Hex;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * 字符编码
 *
 * utf-16 character encoding is variable-length as code points are encoded with one or two 16-bit code units.
 * Each Unicode code point is encoded either as one or two 16-bit code units.
 */
public class CharacterEncodingTest {

    /**
     *
     * 中
     * U+4E2D
     * UTF-16 (hex)	0x4E2D (4e2d)
     * UTF-8 (hex)	0xE4 0xB8 0xAD (e4b8ad)
     * UTF-8 (binary)	11100100:10111000:10101101
     *
     * 𤭢
     * U+24B62
     * UTF-16 (hex)	0xD852 0xDF62 (d852df62)
     * UTF-8 (hex)	0xF0 0xA4 0xAD 0xA2 (f0a4ada2)
     * UTF-8 (binary)	11110000:10100100:10101101:10100010
     */
    @Test
    public void test01() {
        System.out.println("中属于BMP的字符,直接书写");
        System.out.println("codePoint of 中 is " + Integer.toHexString(Character.codePointAt("中", 0)));
        System.out.println("\uD852\uDF62不属于BMP的字符,无法直接书写,只能用\\uHex这种unicode写法");
        System.out.println("codePoint of \uD852\uDF62 is " + Integer.toHexString(Character.codePointAt("\uD852\uDF62", 0)));
    }

    @Test
    public void test02() {
        System.out.println("java.lang.String.getBytes(java.nio.charset.Charset)获取字符串对应字符的相关编码的code-units");
        System.out.println("codePoint of 中 is " + Integer.toHexString(Character.codePointAt("中", 0)));
        byte[] utf16Bytes = "中".getBytes(StandardCharsets.UTF_16);
        System.out.println("in UTF_16 code-units of 中 is " + Hex.encodeHexString(utf16Bytes));
    }

    /**
     * utf-16中一个字符最多拥有2个code-unit
     *
     * 𤭢
     * U+24B62
     * UTF-16 (hex)	0xD852 0xDF62 (d852df62)
     */
    @Test
    public void test16_0() {
        System.out.println("codePoint of \uD852\uDF62 is " + Integer.toHexString(Character.codePointAt("\uD852\uDF62", 0)));

        // 𤭢在utf-16中的code-units是  1101 1000 0101 0010 1101 1111 0110 0010(d852 df62)
        byte[] utf16Bytes = "\uD852\uDF62".getBytes(StandardCharsets.UTF_16);
        System.out.println("in UTF_16 code-units of \uD852\uDF62 is " + Hex.encodeHexString(utf16Bytes));

        // 𤭢在utf-16中的code-units是  1101 1000 0101 0010 1101 1111 0110 0010
        System.out.println("in UTF_16 code-units of \uD852\uDF62 is " + Bin.toBinaryString(utf16Bytes));
    }


    /**
     * 𤭢
     * U+24B62
     *
     * UTF-8 (hex)	0xF0 0xA4 0xAD 0xA2 (f0a4ada2)
     * UTF-8 (binary)	11110000:10100100:10101101:10100010
     *
     */
    @Test
    public void test8_0() {
        System.out.println("codePoint of \uD852\uDF62 is " + Integer.toHexString(Character.codePointAt("\uD852\uDF62", 0)));


        // java.lang.String.getBytes(java.nio.charset.Charset)获取字符串对应字符的相关编码的code-units
        byte[] utf8Bytes = "\uD852\uDF62".getBytes(StandardCharsets.UTF_8);
        System.out.println("in UTF_8 code-units of \uD852\uDF62 is " + Hex.encodeHexString(utf8Bytes));

        // 𤭢在utf-16中的code-units是  1101 1000 0101 0010 1101 1111 0110 0010
        System.out.println("in UTF_8 code-units of \uD852\uDF62 is " + Bin.toBinaryStringWithSeparator(utf8Bytes));
    }
}
