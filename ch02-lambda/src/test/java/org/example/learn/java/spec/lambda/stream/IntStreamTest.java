package org.example.learn.java.spec.lambda.stream;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntStreamTest {

    static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
    }

    /**
     * 统计满足拥有特定特征的数量
     *   使用filter+count
     */
    @Test
    public void test0() {
        Person[] peopleArray = {
                new Person("张三", 25),
                new Person("李四", 30),
                new Person("王五", 28),
                new Person("赵六", 35),
                new Person("孙七", 28)
        };

        long count = Arrays.stream(peopleArray).filter(person -> person.getAge() >= 30).count();
        Assert.assertEquals(2, count);
    }

    /**
     * 统计满足拥有特定特征的数量
     *   使用mapToInt+sum
     */
    @Test
    public void test1() {
        Person[] peopleArray = {
                new Person("张三", 25),
                new Person("李四", 30),
                new Person("王五", 28),
                new Person("赵六", 35),
                new Person("孙七", 28)
        };

        long count = Arrays.stream(peopleArray).mapToInt(person -> person.getAge() >= 30 ? 1 : 0).sum();
        Assert.assertEquals(2, count);
    }
}
