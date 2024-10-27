package org.example.learn.java.lang.spec.other.serialize;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StringSerializeTest {

    @Test
    public void test0() throws IOException {
        String message = "this is a string message";
        String filePath = "string.serialize";
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath)))){
            oos.writeObject(message);
        }
    }
}
