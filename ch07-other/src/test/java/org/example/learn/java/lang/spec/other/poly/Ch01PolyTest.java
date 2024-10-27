package org.example.learn.java.lang.spec.other.poly;

import org.junit.Test;

/**
 *
 * 理解的关键点在于:java的多态
 * 在 Java 中，多态性是在字节码层面通过虚方法表（Virtual Method Table）来实现的.
 * 虚方法表是 Java 虚拟机在加载类的时候创建的一张表，用于存储类的方法信息，包括方法的地址等。
 *
 * 方法的地址的确定: 本类虚表中找不到,再找父类的虚表.
 * 具体的执行过程参考jvm指令集中的invokevirtual
 */
public class Ch01PolyTest {

    private static class ClassA{
        public void method1() {
            System.out.println(this.getClass() + " method1 declared in ClassA");
        }

        public void method2() {
            System.out.println(this.getClass() + " method2 declared in ClassA");
        }

        public void method3() {
            System.out.println(this.getClass() + " method3 declared in ClassA");
            method1();
        }

        public void method4() {
            System.out.println(this.getClass() + " method4 declared in ClassA");
            method2();
        }
    }

    /**
     * 虽然单独看字节码文件,方法引用是固定的,
     * 但是在实际执行时,这些方法的引用地址会指向this所属实际类型的虚方法表中对应方法的地址(这些地址在类加载时才确定,并被改写,从而满足多态特性)
     */
    private static class ClassB extends ClassA {
        public void method1() {
            System.out.println(this.getClass() + " method1 declared in ClassB");
        }

        public void method3() {
            System.out.println(this.getClass() + " method3 declared in ClassB");
            method1(); // 法引用指向的是 com.example.learn.bytecode.cglib.Ch04SelfInvocationTest.ClassB.method1
        }

        public void method4() {
            System.out.println(this.getClass() + " method4 declared in ClassB");
            method2(); // 方法引用指向的是 com.example.learn.bytecode.cglib.Ch04SelfInvocationTest.ClassA.method2
        }

        public void method5() {
            System.out.println(this.getClass() + " method5 declared in ClassB");
            super.method1(); // 方法引用指向的是 com.example.learn.bytecode.cglib.Ch04SelfInvocationTest.ClassA.method1
        }
    }

    private static class ClassC extends ClassA {
        public void method1() {
            System.out.println(this.getClass() + " method1 declared in ClassC");
        }

        public void method3() {
            System.out.println(this.getClass() + " method3 declared in ClassC");
            super.method3();
        }

        public void method4() {
            System.out.println(this.getClass() + " method4 declared in ClassC");
            super.method4();
        }
    }

    @Test
    public void test0() {
        ClassA a = new ClassA();
        a.method3();
        a.method4();
        System.out.println("----------------------------------------------");

        ClassB b = new ClassB();
        b.method3();
        b.method4();
        System.out.println("----------------------------------------------");

        ClassC c = new ClassC();
        c.method3(); // method3 declared in ClassC  -> method3 declared in ClassA -> method1 declared in ClassC
        c.method4(); // method4 declared in ClassC -> method4 declared in ClassA -> method2 declared in ClassA
    }
}
