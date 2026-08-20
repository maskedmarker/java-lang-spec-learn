package org.example.learn.java.lang.spec.io.file.util;


import java.io.File;
import java.io.IOException;

public class PathUtils {

    public static boolean validatePath(String path, String safeBaseDir) throws IOException {
        String safeAbsolutePath = new File(safeBaseDir).getCanonicalPath();
        String absolutePath = new File(path).getCanonicalPath();

        return absolutePath.startsWith(safeAbsolutePath + File.separator);
    }
}
