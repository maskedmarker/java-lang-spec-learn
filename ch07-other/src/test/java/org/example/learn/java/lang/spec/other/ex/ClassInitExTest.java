package org.example.learn.java.lang.spec.other.ex;

import org.junit.Test;

/**
 * 类初始化时抛异常
 */
public class ClassInitExTest {

    private static class BadClass {
        private static int a;
        static {
            System.out.println("BadClass class 将要初始化");
            a = 1/0;
            System.out.println("BadClass class 正常完成初始化");
        }
    }

    @Test(expected = ArithmeticException.class)
    public void test0() {
        int a = 1/0;
    }

    @Test
    public void test1() {
        try {
            System.out.println("将要加载初始化BadClass");
            // 触发class的加载和初始化
            int a = BadClass.a;
            System.out.println("正常加载初始化BadClass");
        } catch (Throwable throwable) {
            // java.lang.ExceptionInInitializerError
            System.out.println("加载初始化BadClass异常. throwable = " + throwable);
        }
        System.out.println("线程正常结束");
    }

    @Test
    public void test2() {
        try {
            System.out.println("将要加载初始化BadClass1");
            // 触发class的加载和初始化
            int a = BadClass.a;
            System.out.println("正常加载初始化BadClass1");
        } catch (Throwable throwable) {
            // java.lang.ExceptionInInitializerError
            System.out.println("加载初始化BadClass异常1. throwable = " + throwable);
        }

        // class-initializer只会执行一次,无论是否失败.所以第二次的引用并不会触发class-initializer再次执行
        try {
            System.out.println("将要加载初始化BadClass2");
            // 触发class的加载和初始化
            int a = BadClass.a;
            System.out.println("正常加载初始化BadClass2");
        } catch (Throwable throwable) {
            // java.lang.NoClassDefFoundError: Could not initialize class org.example.learn.java.lang.spec.other.ex.ClassInitExTest$BadClass
            System.out.println("加载初始化BadClass异常2. throwable = " + throwable);
        }


        System.out.println("线程正常结束");
    }

    @Test
    public void test3() {
        try {
            System.out.println("将要加载初始化BadClass");
            // 另一种触发class的加载和初始化
            Class.forName("org.example.learn.java.lang.spec.other.ex.ClassInitExTest$BadClass");
            System.out.println("正常加载初始化BadClass");
        } catch (Throwable throwable) {
            // java.lang.ExceptionInInitializerError
            System.out.println("加载初始化BadClass异常. throwable = " + throwable);
        }
        System.out.println("线程正常结束");
    }
}
