package org.example.learn.java.spec.reflection.clazz;

/**
 * 关于ClassLoader hierarchy
 *
 * When the runtime environment needs to load a new class for an application, it looks for the class in the following locations, in order:
 * Bootstrap classes:
 *     the runtime classes in rt.jar, internationalization classes in i18n.jar, and others.
 * Installed extensions:
 *     classes in JAR files in the lib/ext directory of the JRE,
 *     and in the system-wide, platform-specific extension directory (such as /usr/jdk/packages/lib/ext on the Solaris™ Operating System, but note that use of this directory applies only to Java™ 6 and later).
 * The class path:
 *     classes, including classes in JAR files, on paths specified by the system property java.class.path.
 *     If a JAR file on the class path has a manifest with the Class-Path attribute, JAR files specified by the Class-Path attribute will be searched also. By default, the java.class.path property's value is ., the current directory. You can change the value by using the -classpath or -cp command-line options, or setting the CLASSPATH environment variable. The command-line options override the setting of the CLASSPATH environment variable.
 */
public class ClassLoaderHierarchyDemo {

    /**
     *                                              null(BootstrapClassLoader)
     *                                             -|
     *                                ExtClassLoader
     * AppClassLoader(即SystemClassLoader) -|
     */
    public void displayBuiltInClassLoaders() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        ClassLoader temp = classLoader;
        int index = 0;
        do {
            System.out.println(String.format("index=%d temp=%s", index++, temp));
        } while ((temp = temp.getParent()) != null);
    }

    public void systemClassLoader() {
        // AppClassLoader也被称作系统类加载器,这里的system指的是应用系统
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        System.out.println("systemClassLoader = " + systemClassLoader);

        // system ClassLoader的父ClassLoader就是ExtClassLoader,即扩展类加载器
        ClassLoader systemClassLoaderParent = systemClassLoader.getParent();
        System.out.println("systemClassLoaderParent = " + systemClassLoaderParent);
    }
}
