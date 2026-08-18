package org.example.learn.java.spec.reflection.generic;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * java.lang.Class#getGenericSuperclass 方法的返回值类型是 java.lang.reflect.Type。
 * 这是一个接口，而不是具体的类。它用于获取当前类（Class 对象所代表的类）的带有泛型参数信息的父类类型。
 * 当父类没有泛型参数时，返回的就是普通的 Class 对象。
 * 当父类带有泛型参数时，返回这个接口的实现，可以从中提取出具体的泛型类型。
 */
public class GenericSuperclassTest {

    // 情况1：父类没有泛型
    class ThreadChild extends Thread {
    }

    // 情况2：父类有泛型
    class MyList extends ArrayList<String> {
    }


    @Test
    public void test1() {
        // 情况1：父类无泛型 -> 返回 Class
        Type type1 = ThreadChild.class.getGenericSuperclass();
        Assert.assertSame(Thread.class, type1);


        // 情况2：父类有泛型 -> 返回 ParameterizedType
        Type type2 = MyList.class.getGenericSuperclass();
        Assert.assertTrue(type2 instanceof ParameterizedType);

        // 从 ParameterizedType 中提取泛型参数
        if (type2 instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type2;
            // 获取父类的原始类型（ArrayList）
            System.out.println(pt.getRawType());   // class java.util.ArrayList
            // 获取泛型参数列表（String）
            Type[] actualTypeArgs = pt.getActualTypeArguments();
            System.out.println(actualTypeArgs[0]); // class java.lang.String
        }
    }
}
