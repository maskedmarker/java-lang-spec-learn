package org.example.learn.java.spec.reflection.caller;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import java.util.Arrays;

/*
这个注解是为了堵住安全漏洞用的。
曾经有黑客通过构造双重反射来提升权限，原理是当时反射只检查固定深度的调用者的类，看它有没有特权，例如固定看两层的调用者（getCallerClass(2)）。
如果我的类本来没足够权限群访问某些信息，那我就可以通过双重反射去达到目的：
反射相关的类是有很高权限的，而在 我->反射1->反射2 这样的调用链上，反射2检查权限时看到的是反射1的类，这就被欺骗了，导致安全漏洞。
使用CallerSensitive后，getCallerClass不再用固定深度去寻找actual caller（“我”），
而是把所有跟反射相关的接口方法都标注上CallerSensitive，搜索时凡看到该注解都直接跳过，这样就有效解决了前面举例的问题

Reflection.getCallerClass()要求调用者的方法必须有@CallerSensitive注解，并且必须有权限（由bootstrap class loader或者extension class loader加载的类）才可以调用.
主要还是JDK底层控制权限的地方使用。
与我们日常开发的反射无关.
*/
public class CallerDemo {

    public static void main(String[] args) {
        CallerDemo demo = new CallerDemo();
        demo.demoCallerWithCallerSensitive();
    }

    public void demoCallerWithoutCallerSensitive() {
        InvokerClass invokerObject = new InvokerClass();
        InvokedClass invokedObject = new InvokedClass();
        invokerObject.doFoo(invokedObject); // CallerSensitive annotation expected at frame 1
    }

    public void demoCallerWithCallerSensitive() {
        InvokerClass invokerObject = new InvokerClass();
        InvokedClass invokedObject = new InvokedClass();
        invokerObject.doFooWithCallerSensitive(invokedObject);
    }


    private static class InvokerClass {
        public void doFoo(InvokedClass invokedCalss) {
            invokedCalss.doBar();
        }

        public void doFooWithCallerSensitive(InvokedClass invokedClass) {
            invokedClass.doBarWithCallerSensitive();
        }
    }

    private static class InvokedClass {
        public void doBar() {
            Class<?> callerClass = Reflection.getCallerClass();
            System.out.printf("%s called the method %s of %s%n", callerClass, "doBar", this.getClass());
        }

        @CallerSensitive
        public void doBarWithCallerSensitive() {
            Class<?> callerClass;
            try {
                ClassLoader classLoader = this.getClass().getClassLoader(); // AppClassLoader
                System.out.println("classLoader = " + classLoader);
                callerClass = Reflection.getCallerClass();
            } catch (Throwable e) {
                System.out.println("---------------------------");
                Arrays.stream(e.getStackTrace()).forEach(System.out::println);
                System.out.println("---------------------------");
                throw e;
            }

            System.out.printf("%s called the method %s of %s%n", callerClass, "doBarWithCallerSensitive", this.getClass());
        }
    }
}
