package org.example.learn.java.lang.spec.io.file;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class PathTest {

    @Test
    public void test0() {
        String cwd = System.getProperty("user.dir");
        System.out.println("Paths.get(cwd).toAbsolutePath() = " + Paths.get(cwd).toAbsolutePath());

        String safeDir = cwd + "/" + "dir1/dir11/dir111/";
        String safeAbsolutePath = new File(safeDir).getAbsolutePath();
        System.out.println("safeAbsolutePath = " + safeAbsolutePath);

        Assert.assertFalse(safeAbsolutePath.endsWith("/"));
    }

    /**
     * 目录的getAbsolutePath()返回路径结尾不带分隔符
     */
    @Test
    public void test1() {
        String cwd = System.getProperty("user.dir");
        System.out.println("Paths.get(cwd).toAbsolutePath() = " + Paths.get(cwd).toAbsolutePath());

        String safeDir = cwd + "/" + "dir1/dir11/../dir111/";
        String safeAbsolutePath = new File(safeDir).getAbsolutePath();
        System.out.println("safeAbsolutePath = " + safeAbsolutePath);

        Assert.assertTrue(safeAbsolutePath.contains(".."));
    }

    /**
     *
     * getCanonicalPath()
     * This method first converts this pathname to absolute form if necessary, as if by invoking the getAbsolutePath method,
     * and then maps it to its unique form in a system-dependent way.
     *
     * This typically involves:
     * removing redundant names such as "." and ".." from the pathname,
     * resolving symbolic links (on UNIX platforms),
     * and converting drive letters to a standard case (on Microsoft Windows platforms).
     */
    @Test
    public void test2() throws IOException {
        String cwd = System.getProperty("user.dir");
        System.out.println("Paths.get(cwd).toAbsolutePath() = " + Paths.get(cwd).toAbsolutePath());

        String safeDir = cwd + "/" + "dir1/dir11/../dir111/";
        String safeAbsolutePath = new File(safeDir).getCanonicalPath();
        System.out.println("safeAbsolutePath = " + safeAbsolutePath);

        Assert.assertFalse(safeAbsolutePath.contains(".."));
    }
}
