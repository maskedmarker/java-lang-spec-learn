package org.example.learn.java.spec.lambda;

import org.junit.Test;

import java.util.function.BiPredicate;
import java.util.function.Function;

public class MethodReferenceTest {

    static <R> R playOneArgument(String s1, Function<String, R> func) {
        return func.apply(s1);
    }

    static Boolean playTwoArgument(String s1, String s2, BiPredicate<String, String> func) {
        return func.test(s1, s2);
    }

    /**
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
}
