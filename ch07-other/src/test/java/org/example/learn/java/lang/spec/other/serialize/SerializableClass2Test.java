package org.example.learn.java.lang.spec.other.serialize;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SerializableClass2Test {

    private static final String FILE_NAME_SUFFIX = "ser";

    @Test
    public void testSerialization() throws IOException {
        SerializableClass2 person = new SerializableClass2();
        person.setId(1024);
        person.setName("zhangsan");
        System.out.println("person = " + person);

        String objectFileName = SerializableClass2.class.getSimpleName() + FILE_NAME_SUFFIX;
        ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(objectFileName)));
        oos.writeObject(person);
    }

    @Test
    public void testDeserialization() throws IOException, ClassNotFoundException {
        String objectFileName = SerializableClass2.class.getSimpleName() + FILE_NAME_SUFFIX;
        ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(objectFileName)));
        SerializableClass2 person = (SerializableClass2) ois.readObject();
        System.out.println("person = " + person);
    }
}