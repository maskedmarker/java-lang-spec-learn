package org.example.learn.java.lang.spec.other.serialize;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 对象序反序列化安全问题
 * <p>
 * note:
 */
public class SerializationSecurityTest {

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
        ValidatingObjectInputStream in = new ValidatingObjectInputStream(Files.newInputStream(Paths.get(objectFileName)));
        in.accept(SerializableClass1.class);
        SerializableClass1 person = (SerializableClass1) in.readObject();
        System.out.println("person = " + person);
    }
}