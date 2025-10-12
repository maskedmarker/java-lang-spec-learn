package org.example.learn.java.spec.reflection.generic;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * java.lang.reflect.Field#getGenericType 和java.lang.reflect.Field#getType方法有什么不同
 * getType() Returns the raw (erased) type of the field.
 * getGenericType() Returns the full generic type of the field, including generic parameters if present.
 *
 *
 *
 * @param <T>
 */
public class FieldGenericTypeInfoTest1<T> {

    // 属性的类型可以有 普通类型/泛型/类型变量(反射语境下叫类型变量typeVariable,泛型语境下叫做typeParameter)
    String nonGenericField;
    List<String> genericField;
    T typeVariableField;

    /**
     * 当属性的类型是非泛型类型时, getType()和getGenericType()返回的都是Class<?>类型,且为同一个Class<?>的实例
     *
     * @throws NoSuchFieldException
     */
    @Test
    public void test0() throws NoSuchFieldException {
        Field nonGenericField = FieldGenericTypeInfoTest1.class.getDeclaredField("nonGenericField");
        System.out.println("nonGenericField.getGenericType().getClass() = " + nonGenericField.getGenericType() + "  |  " + nonGenericField.getGenericType().getClass());
        System.out.println("nonGenericField.getType().getClass() = " + nonGenericField.getType());
        Assert.assertEquals("当属性的类型是非泛型类型时, getType()和getGenericType()返回的都是Class<?>类型,且为同一个Class<?>的实例", nonGenericField.getType(), nonGenericField.getGenericType());

        System.out.println("-----------------------------");

        Field genericField = FieldGenericTypeInfoTest1.class.getDeclaredField("genericField");
        System.out.println("genericField.getGenericType() = " + genericField.getGenericType() + "  |  " + genericField.getGenericType().getClass());
        System.out.println("genericField.getType() = " + genericField.getType());
        Assert.assertNotEquals("当属性的类型是泛型类型时, getType()和getGenericType()返回的一个是Class<?>类型,一个是ParameterizedType类型,且不是同一个的实例", genericField.getType(), genericField.getGenericType());

        System.out.println("-----------------------------");

        Field typeVariableField = FieldGenericTypeInfoTest1.class.getDeclaredField("typeVariableField");
        System.out.println("typeVariableField.getGenericType() = " + typeVariableField.getGenericType() + "  |  " + typeVariableField.getGenericType().getClass());
        System.out.println("typeVariableField.getType() = " + typeVariableField.getType());
        Assert.assertNotEquals("当属性的类型是TypeVariable类型时, getType()和getGenericType()返回的一个是Class<?>类型,一个是TypeVariable类型,且必然不是同一个对象", typeVariableField.getType(), typeVariableField.getGenericType());
    }
}
