package org.example.learn.java.spec.lambda;

import org.junit.Test;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterTest {

    @Test
    public void testOr() {
        Predicate<String> containsA = i -> i.contains("a");
        Predicate<String> containsB = i -> i.contains("b");
        Predicate<String> containsC = i -> i.contains("c");

        Predicate<String> containsAbc = containsA.or(containsB).or(containsC);
        List<String> strings = Stream.of("abc", "ac", "cd", "e").filter(containsAbc).collect(Collectors.toList());
        System.out.println("strings = " + strings);

        strings = Stream.of("abc", "ac", "cd", "e").filter(containsAbc.negate()).collect(Collectors.toList());
        System.out.println("strings = " + strings);
    }
}
