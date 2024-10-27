package org.example.learn.java.spec.reflection.clazz;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

public class BridgeMethodTest {

    public static class Parent {
        public Number get() {
            return 1;
        }

        public String toString() {
            return "1";
        }
    }

    public static class Child extends Parent {

        @Override
        public Integer get() {
            return 1;
        }
    }

    private void logMethodInfo(Method method) {
        String msg = String.format("method signature: %s | modifier: isSynthetic=%b isBridge=%b", method, method.isSynthetic(), method.isBridge());
        System.out.println(msg);
    }

    /**
     * 协变返回类型是指子类方法的返回值类型不必严格等同于父类中被重写的方法的返回值类型,而可以是更 “具体” 的类型.
     * 有一个方法java.lang.Number get(), 在Child源码中是没有出现过的,是由编译器自动生成的,该方法被标记为ACC_BRIDGE和ACC_SYNTHETIC,就是我们前面所说的桥接方法.
     */
    @Test
    public void test0() {
        Arrays.stream(Parent.class.getDeclaredMethods()).forEach(this::logMethodInfo);
        System.out.println("-----------------------");
        Arrays.stream(Child.class.getDeclaredMethods()).forEach(this::logMethodInfo);
    }

    /**
     * 在getMethods中不会出现父类的被override方法
     */
    @Test
    public void test1() {
        Arrays.stream(Parent.class.getMethods()).forEach(this::logMethodInfo);
        System.out.println("-----------------------");
        Arrays.stream(Child.class.getMethods()).forEach(this::logMethodInfo);
    }
}
