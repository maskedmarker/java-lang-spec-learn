package org.example.learn.java.lang.spec.other.serialize;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * 被序列化的类
 * <p>
 * note: 通过反序列化来实例化对象的时候,
 * 1. 对象实例不通过构造函数,且不调用无参构造函数
 * 2. 字段赋值也不通过setter方法
 */
public class SerializableClass1 implements Serializable {

    private static final long serialVersionUID = 3408169507716120674L;

    private int id;
    private String name;

    public SerializableClass1() {
        System.out.println(SerializableClass1.class.getSimpleName() + " is instantiating");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .toString();
    }
}
