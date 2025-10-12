package org.example.learn.java.spec.reflection.generic;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
public class FieldGenericTypeInfoTest2<T> {

    // 属性的类型可以有 普通类型/泛型/类型变量(反射语境下叫类型变量typeVariable,泛型语境下叫做typeParameter)
    String nonGenericField;
    List<String> genericField1;
    List<String> genericField2;
    List<Integer> genericField3;
    T typeVariableField;

    @Test
    public void test0() throws NoSuchFieldException {
        Field genericField1 = FieldGenericTypeInfoTest2.class.getDeclaredField("genericField1");
        System.out.println("genericField.getGenericType() = " + genericField1.getGenericType() + "  |  " + genericField1.getGenericType().getClass());
        Field genericField2 = FieldGenericTypeInfoTest2.class.getDeclaredField("genericField2");
        System.out.println("genericField2.getGenericType() = " + genericField2.getGenericType() + "  |  " + genericField2.getGenericType().getClass());


        Assert.assertEquals("当属性的类型是泛型类型时,且泛型的类型变量是相同的类型, getGenericType()返回都是ParameterizedType类型,且一定是equals的实例",
                genericField1.getGenericType(), genericField2.getGenericType());
        Assert.assertNotSame("可能不是同一个实例,但一定是equals的实例", genericField1.getGenericType(), genericField2.getGenericType()); // 可以看一下ParameterizedTypeImpl的equals方法

        System.out.println("-----------------------------");

        Field genericField3 = FieldGenericTypeInfoTest2.class.getDeclaredField("genericField3");
        System.out.println("genericField3.getGenericType() = " + genericField3.getGenericType() + "  |  " + genericField3.getGenericType().getClass());
        Assert.assertNotEquals("当属性的类型是泛型类型时,且泛型的类型变量不是相同的类型, getGenericType()返回都是ParameterizedType类型,且一定不是equals的实例", genericField3, genericField1);
        Assert.assertNotSame("不可能是同一个实例,且一定不是equals的实例", genericField1.getGenericType(), genericField2.getGenericType());

        System.out.println("-----------------------------");
    }

    @Test
    public void test01() throws NoSuchFieldException {
        Field genericField1 = FieldGenericTypeInfoTest2.class.getDeclaredField("genericField1");
        Field genericField2 = FieldGenericTypeInfoTest2.class.getDeclaredField("genericField2");
        Field genericField3 = FieldGenericTypeInfoTest2.class.getDeclaredField("genericField3");

        if (genericField1.getGenericType() instanceof ParameterizedType) {
            Type actualTypeArgument = ((ParameterizedType) genericField1.getGenericType()).getActualTypeArguments()[0];
            System.out.println("genericField1 actualTypeArgument = " + actualTypeArgument);
        }
        if (genericField2.getGenericType() instanceof ParameterizedType) {
            Type actualTypeArgument = ((ParameterizedType) genericField2.getGenericType()).getActualTypeArguments()[0];
            System.out.println("genericField2 actualTypeArgument = " + actualTypeArgument);
        }
        if (genericField3.getGenericType() instanceof ParameterizedType) {
            Type actualTypeArgument = ((ParameterizedType) genericField3.getGenericType()).getActualTypeArguments()[0];
            System.out.println("genericField3 actualTypeArgument = " + actualTypeArgument);
        }
    }

    @Test
    public void test02() throws NoSuchFieldException {
        Field typeVariableField = FieldGenericTypeInfoTest2.class.getDeclaredField("typeVariableField");

        if (typeVariableField.getGenericType() instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) typeVariableField.getGenericType();
            System.out.println("typeVariable.getName() = " + typeVariable.getName());

            System.out.println("-----------------------------");

            for (Type bound : typeVariable.getBounds()) {
                System.out.println("bound = " + bound);
            }

            System.out.println("-----------------------------");

            GenericDeclaration typeVariableGenericDeclaration = typeVariable.getGenericDeclaration();
            System.out.println("typeVariableGenericDeclaration = " + typeVariableGenericDeclaration);
            Assert.assertSame("typeVariable.getGenericDeclaration()返回的是声明这个typeVariable的类", FieldGenericTypeInfoTest2.class, typeVariableGenericDeclaration);
        }
    }
}
