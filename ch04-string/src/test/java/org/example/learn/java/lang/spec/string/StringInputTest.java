package org.example.learn.java.lang.spec.string;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * A compiler for the Java programming language ("Java compiler") first recognizes Unicode escapes in its input,
 * translating the ASCII characters \\u and four hexadecimal digits followed to the UTF-16 code unit.
 * Representing supplementary characters requires two consecutive Unicode escapes.
 *
 * This translation step results in a sequence of Unicode input characters:
 *
 * UnicodeInputCharacter:
 * 	UnicodeEscape
 * 	RawInputCharacter
 * UnicodeEscape:
 * 	\ UnicodeMarker HexDigit HexDigit HexDigit HexDigit
 * UnicodeMarker:
 * 	u {u}
 * HexDigit:
 * 	(one of)
 * 	0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
 * RawInputCharacter:
 * 	any Unicode character
 *
 * The \, u, and hexadecimal digits here are all ASCII characters.
 *
 * 注意:
 * 该类的源码第八行中出现了\\u.如果只有一个反斜杠,则会在编译源码时,出现[非法的 Unicode 转义].
 * 这充分体现了,Java compiler在编译的第一步就是将java文件整体作为文本输入(不区分注释和java代码),
 * (无论何种编码,对应的字符是确定的)正常情况下会将读取到的字符直接使用,
 * 但是当读取到反斜杠及其紧跟的字母u时,会将这串字符转换为对应的字符,再做使用(即转义).
 */
public class StringInputTest {

    @Test
    public void test0() {
        // 编译器读取到的并非看到的字符,而是转义后的字符
        String stringInput = "\u64CD\u4F5C";
        System.out.println("stringInput = " + stringInput);
    }

    @Test
    public void test1() throws IOException {
        InputStream inputStream = StringInputTest.class.getResourceAsStream("tmp.properties");
        Properties properties = new Properties();
        // The input stream is in a simple line-oriented format as specified in load(Reader)
        // and is assumed to use the ISO 8859-1 character encoding
        // that is each byte is one Latin1 character.
        // Characters not in Latin1, and certain special characters, are represented in keys and elements using Unicode escapes as defined
        properties.load(inputStream);
        properties.entrySet().forEach(System.out::println);
    }

    @Test
    public void test2() throws IOException {
        InputStream inputStream = StringInputTest.class.getResourceAsStream("tmp2.properties");
        Properties properties = new Properties();
        properties.load(inputStream);
        properties.entrySet().forEach(System.out::println);
    }
}
