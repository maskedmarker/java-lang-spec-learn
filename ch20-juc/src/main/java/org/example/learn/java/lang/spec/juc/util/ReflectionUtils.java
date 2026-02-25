package org.example.learn.java.lang.spec.juc.util;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static <T, V> V getFieldValue(T instance, String fieldName) {

        if (instance == null) {
            throw new IllegalArgumentException("对象不能为null");
        }

        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("字段名不能为空");
        }

        try {
            Field field = getDeclaredField(instance.getClass(), fieldName);
            if (field == null) {
                throw new NoSuchFieldException("在类及其父类中未找到字段: " + fieldName);
            }
            field.setAccessible(true);
            return (V)  field.get(instance);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 递归查找字段（包括父类）
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName) {
        // 在当前类中查找
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // 如果当前类没有，尝试在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return getDeclaredField(superClass, fieldName);
            }

            return null;
        }
    }
}
