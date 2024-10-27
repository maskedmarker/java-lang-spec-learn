package org.example.learn.java.lang.spec.other.resource;

import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceTest {

    @Test
    public void test0() throws URISyntaxException {
        String canonicalName = ResourceTest.class.getCanonicalName();
        System.out.println("canonicalName = " + canonicalName);
        String filePath = canonicalName.replace(".", File.separator) + ".class";
        System.out.println("filePath = " + filePath);

        //
        URL resource = ResourceTest.class.getClassLoader().getResource(filePath);
        System.out.println("resource = " + resource);
        URI uri = resource.toURI();
        System.out.println("uri = " + uri);
        String path = uri.getPath();
        System.out.println("path = " + path);
    }

    @Test
    public void test1() throws URISyntaxException {
        String canonicalName = ResourceTest.class.getCanonicalName();
        System.out.println("canonicalName = " + canonicalName);
        String filePath = canonicalName.replace(".", File.separator) + ".class";
        System.out.println("filePath = " + filePath);
        URL resource = ResourceTest.class.getClassLoader().getResource(filePath);
        System.out.println("resource = " + resource);
        URI uri = resource.toURI();
        System.out.println("uri = " + uri);
        String path = uri.getPath();
        System.out.println("path = " + path);

        Path pathObj = Paths.get(path);
        System.out.println("pathObj.toString() = " + pathObj.toString());

    }
}
