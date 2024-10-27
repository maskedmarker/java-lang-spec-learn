package org.example.learn.java.spec.reflection.clazz;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Class#getResourceAsStream(java.lang.String) 该方法会自动将该Class实例的包名作为路劲的前缀,补充到入参前;而ClassLoader就没有这个特性
 */
public class LoadResourceTest {

    @Test
    public void test0() {
        URL url = LoadResourceTest.class.getResource("resource.txt");
        Assert.assertNotNull(url);
        System.out.println("url = " + url);
    }

    @Test
    public void test1() throws IOException {
        InputStream resourceAsStream = LoadResourceTest.class.getResourceAsStream("resource.txt");
        Assert.assertNotNull(resourceAsStream);

        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}
