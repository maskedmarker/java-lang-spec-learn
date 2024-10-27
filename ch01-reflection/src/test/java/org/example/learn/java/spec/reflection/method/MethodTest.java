package org.example.learn.java.spec.reflection.method;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * <method signature>:=<type parameters><name><parameter types>
 */
public class MethodTest {

    @Test
    public void testMethodName() {
        System.out.println("Method.getName() 仅返回方法签名的名称");
        Method[] declaredMethods = Object.class.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            System.out.println("java.lang.reflect.Method.getName() = " + declaredMethod.getName());
        }
    }

}