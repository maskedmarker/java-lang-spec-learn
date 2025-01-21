package org.example.learn.java.spec.lambda.methodreference;

import org.junit.Test;

import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * 在 Java 中，Lambda 表达式可以用来表示一个函数式接口的实现。如果你希望将对象的方法直接作为 Lambda 表达式使用，可以通过 方法引用来实现
 * <p>
 * 方法引用的语法格式为：
 * ClassName::methodName
 * instanceName::methodName
 */
public class MethodReferenceTest {

    static <R> R playOneArgument(String param, Function<String, R> func) {
        return func.apply(param);
    }

    static Boolean playTwoArgument(String param1, String param2, BiPredicate<String, String> func) {
        return func.test(param1, param2);
    }

    /**
     * 使用lambda替代method reference,做等价转换
     * 注意参数个数
     */
    @Test
    public void test0() {
        // lambda
        int result = playOneArgument("mkyong", x -> x.length());   // 6
        // method reference
        int result2 = playOneArgument("mkyong", String::length);   // 6

        // lambda
        Boolean result3 = playTwoArgument("mkyong", "y", (a, b) -> a.contains(b)); // true
        // method reference
        Boolean result4 = playTwoArgument("mkyong", "y", String::contains);        // true

        // lambda
        Boolean result5 = playTwoArgument("mkyong", "1", (a, b) -> a.startsWith(b)); // false
        // method reference
        Boolean result6 = playTwoArgument("mkyong", "y", String::startsWith);        // false

        System.out.println(result6);
    }

    private static class StringProcessor {
        public String process(String input) {
            return input.toUpperCase();
        }
    }

    @Test
    public void test1() {
        // 创建 StringProcessor 实例
        StringProcessor processor = new StringProcessor();

        // 定义一个 Function 接口，接受一个 String 并返回一个 String
        Function<String, String> function = processor::process;

        // 使用 Lambda 表达式调用方法
        String result = function.apply("hello");
        System.out.println(result); // 输出: HELLO
    }
}
