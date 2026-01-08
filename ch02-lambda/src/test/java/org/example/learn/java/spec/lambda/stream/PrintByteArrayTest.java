package org.example.learn.java.spec.lambda.stream;

import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 有多种方式可以通过 lambda 表达式将 byte[] 的每个元素打印出来
 */
public class PrintByteArrayTest {

    // 辅助方法：byte[] 转 int[]
    private static int[] toIntArray(byte[] bytes) {
        int[] ints = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            ints[i] = bytes[i] & 0xFF; // 消除符号影响
        }
        return ints;
    }

    // 辅助方法：byte[] 转 Byte[]
    private static Byte[] toByteArray(byte[] bytes) {
        Byte[] result = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }

    /**
     * 使用 IntStream
     */
    @Test
    public void test01() {
        byte[] bytes = {65, 66, 67, 68, 69, 70}; // ABCDEF

        System.out.println("=== 使用 IntStream ===");
        // byte[] 没有 stream() 方法，需要转换为 IntStream
        Arrays.stream(toIntArray(bytes)).forEach(b -> System.out.println((char) b));
    }

    /**
     * 使用IntStream.range替换for循环
     */
    @Test
    public void test02() {
        byte[] bytes = {65, 66, 67, 68, 69, 70}; // ABCDEF

        System.out.println("=== 使用循环索引 ===");
        IntStream.range(0, bytes.length).forEach(i -> System.out.println((char) bytes[i]));
    }

    /**
     * jdk中只提供了IntStream和LongStream,没有char/short/float/double之类是stream
     * char/short之类的可以用IntStream来完成
     * char/short/float/double之类也可以使用object-stream,即使用包装类
     */
    @Test
    public void test11() {
        byte[] bytes = {65, 66, 67, 68, 69, 70}; // ABCDEF


        System.out.println("=== 使用包装类 ===");
        // 需要先转换为 Byte[] 对象数组
        Byte[] byteObjects = toByteArray(bytes);
        Arrays.stream(byteObjects).forEach(i -> System.out.printf("%s\n", (char)(byte)i));
    }

    /**
     * 拼接后打印
     * 类似于StringJoiner
     */
    @Test
    public void test21() {
        byte[] bytes = {65, 66, 67, 68, 69, 70}; // ABCDEF

        String result = Arrays.stream(toIntArray(bytes)).mapToObj(b -> String.format("%s", Character.toString((char) b))).collect(Collectors.joining(" ", "[", "]"));
        System.out.println(result);
    }
}
