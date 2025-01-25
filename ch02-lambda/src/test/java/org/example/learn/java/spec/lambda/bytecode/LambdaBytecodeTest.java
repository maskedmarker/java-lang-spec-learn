package org.example.learn.java.spec.lambda.bytecode;

import org.junit.Test;

public class LambdaBytecodeTest {

    private static class MyClass {
        static void printMessage() {
            System.out.println("Hello, World!");
        }
    }

    @Test
    public void test0() {
        Runnable r = MyClass::printMessage;  // Method reference
        r.run();
    }

    @Test
    public void test1() {
        Runnable r = () -> System.out.println("hello world");  // lambda expression
        r.run();
    }
}
