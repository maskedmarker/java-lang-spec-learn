package org.example.learn.java.lang.spec.other.serialize;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 对象序列化和反序列化
 * <p>
 * note: 通过反序列化来实例化对象的时候,
 * 1. 对象实例不通过构造函数
 * 2. 字段赋值也不通过setter方法
 */
public class SerializableClass1Test {

    private static final String FILE_NAME_SUFFIX = "ser";

    @Test
    public void testSerialization() throws IOException {
        SerializableClass1 person = new SerializableClass1();
        person.setId(1024);
        person.setName("zhangsan");
        System.out.println("person = " + person);

        String objectFileName = SerializableClass1.class.getSimpleName() + FILE_NAME_SUFFIX;
        ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(objectFileName)));
        oos.writeObject(person);
    }

    @Test
    public void testDeserialization() throws IOException, ClassNotFoundException {
        String objectFileName = SerializableClass1.class.getSimpleName() + FILE_NAME_SUFFIX;
        ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(objectFileName)));
        SerializableClass1 person = (SerializableClass1) ois.readObject();
        System.out.println("person = " + person);
    }
}