package org.example.learn.java.spec.lambda;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BiConsumerTest {

    private static class User {
        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    /**
     * 将实例方法(非方法)应用于BiConsumer时,实例方法的形参只能有一个
     */
    @Test
    public void test0() {
        // BiConsumer用于实例方法(非方法)时,编译器就认定你BiConsumer.accept的第一arg是实例对象,第二个arg是实例方法的唯一实参
        BiConsumer<User, Integer> idSetter = User::setId;
        User user = new User();
        idSetter.accept(user, 1);
        Assert.assertEquals(1, (int) user.getId());
    }

    /**
     *
     */
    @Test
    public void test1() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        // Define a BiConsumer that prints each key-value pair
        BiConsumer<String, Integer> printConsumer = (key, value) -> System.out.println(key + ": " + value);

        // Use the BiConsumer with the map
        map.forEach(printConsumer);
    }


    private static class MyConsumer {
        public void accept(int i) {}
        public static void biAccept(MyConsumer mc, int i) {}
    }

    private static class BiConsumerDemo {
        public void accept(int i) {}
        public static void biAccept(MyConsumer mc, int i) {}
    }
    /**
     * Why is BiConsumer allowed to be assigned with a function that only accepts a single parameter?
     *
     * (from 15.13.1. Compile-Time Declaration of a Method Reference )
     * given a targeted function type with n parameters, a set of potentially applicable methods is identified:
     * If the method reference expression has the form ReferenceType :: [TypeArguments] Identifier, the potentially applicable methods are the member methods of the type to search that have an appropriate name (given by Identifier), accessibility, arity (n or n-1), and type argument arity (derived from [TypeArguments]), as specified in §15.12.2.1.
     * Two different arities, n and n-1, are considered, to account for the possibility that this form refers to either a static method or an instance method.
     *
     * BiConsumer如果被赋值了non-static method reference which has only one parameter,就是将this当作第一个parameter
     */
    @Test
    public void test2() {
        BiConsumer<MyConsumer, Integer> accumulator = (x, y) -> {}; // no problem, method accepts 2 parameters

        // the target function type is BiConsumer, which has 2 parameters.
        // Therefore any methods of the MyConsumer class matching the name accept and having 2 or 1 parameters are considered.
        accumulator = MyConsumer::accept; // accepts only one parameter and yet is treated as BiConsumer

        // Both static methods having 2 parameters and instance methods having 1 parameter can match the target function type.
        accumulator = MyConsumer::biAccept; // needed to be static

        // 这里编译错误是因为变量accumulator的第一个type parameter是MyConsumer
        //accumulator = BiConsumerDemo::accept; // compilation error, only accepts single parameter
        BiConsumer<BiConsumerDemo, Integer> accumulator2;
        accumulator2 = BiConsumerDemo::accept;
    }
}
