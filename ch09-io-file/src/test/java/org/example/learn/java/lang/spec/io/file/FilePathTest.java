package org.example.learn.java.lang.spec.io.file;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

public class FilePathTest {

    @Test
    public void test0() {
        String userDir = System.getProperty("user.dir");
        System.out.println("userDir = " + userDir);

        File file = new File("");
        // 注意看getAbsolutePath的方法注释
        String absolutePath = file.getAbsolutePath();
        System.out.println("absolutePath = " + absolutePath);

        Assert.assertEquals(userDir, absolutePath);
    }
}
