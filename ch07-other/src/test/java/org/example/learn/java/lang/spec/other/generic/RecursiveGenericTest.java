package org.example.learn.java.lang.spec.other.generic;

import org.junit.Test;

/**
 * 不要纠结于“T 到底是谁”的无限递归,把它看作一种java语法的固定语言规范(契约或承诺)
 * 递归泛型的核心目的是在继承关系中“锁定”具体的类型,确保在父类中定义的方法（如compareTo）中参数类型与最终子类的类型一致,从而实现编译时的类型安全.
 */
public class RecursiveGenericTest {

    static class Animal<T extends Animal<T>> implements Comparable<T> {
        protected String name;
        protected int weight;

        @Override
        public int compareTo(T other) {
            // 现在,other 的类型就是 T,而不是笼统的 Animal
            // 这意味着它一定是当前类的具体子类型（比如Dog）
            return Integer.compare(this.weight, other.weight);
        }
    }

    static class Dog extends Animal<Dog> {
        public void fetch() {
            System.out.println("Fetching the ball!");
        }
    }

    static class Cat extends Animal<Cat> {
        public void meow() {
            System.out.println("Meow!");
        }
    }

    @Test
    public void test0() {
        Dog myDog = new Dog();
        Dog yourDog = new Dog();
        Cat myCat = new Cat();

        myDog.compareTo(yourDog); // ✅ 完美！编译通过,类型安全
//        myDog.compareTo(myCat);   // ❌ 编译错误！编译器直接报错
    }
}
