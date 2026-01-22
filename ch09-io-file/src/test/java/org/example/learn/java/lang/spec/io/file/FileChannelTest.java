package org.example.learn.java.lang.spec.io.file;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * mmap是一个非常重要的系统调用,用于内存映射文件或设备到进程的地址空间. 它提供了一种高效的文件I/O方式,避免了传统的read/write系统调用带来的数据拷贝开销.
 * <p>
 * 将文件或设备映射到进程的虚拟内存空间;创建匿名的内存区域(不关联文件);
 */
public class FileChannelTest {

    /**
     * 获取FileChannel对象
     * FileChannel.open
     * new FileInputStream(filePath).getChannel()
     */
    @Test
    public void test01() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(cwd, "src/test/resources/mmap.file");

        // 不要使用: FileChannel fc = new FileInputStream(filePath).getChannel() 因为无法控制读写权限
        FileChannel ch = FileChannel.open(path, StandardOpenOption.READ);
        Assert.assertNotNull(ch);

        // 使用完后需要close关闭
        ch.close();
    }

    /**
     * FileChannel.size()方法会发生系统调用,查询底层文件的大小
     */
    @Test
    public void test02() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(cwd, "src/test/resources/mmap.file");
        FileChannel ch = FileChannel.open(path, StandardOpenOption.READ);

        // size()方法会发生系统调用,查询底层文件的大小
        long fileSize = ch.size();
        System.out.println("fileSize = " + fileSize);

        // 使用完后需要close关闭
        ch.close();
    }

    // region 读取操作

    /**
     * 读取FileChannel内容
     *      分块读取文件内容
     */
    @Test
    public void test11() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(cwd, "src/test/resources/mmap.file");

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {       // FileChannel是AutoClosable
            System.out.println("文件内容如下:");

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead;
            while ((bytesRead = ch.read(buffer)) != -1) {
                buffer.flip(); // 切换到读模式
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                System.out.println(new String(bytes, StandardCharsets.UTF_8));

                buffer.flip(); // 切换到写模式
            }
        }
    }

    /**
     * 读取FileChannel内容
     *      分块读取文件内容(优化缓存,使用compact)
     */
    @Test
    public void test12() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(cwd, "src/test/resources/mmap.file");

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            System.out.println("文件内容如下:");

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead;
            while ((bytesRead = ch.read(buffer)) != -1) {                     // 读取文件,直到eof (end of file)
                buffer.flip(); // 准备读取缓冲区数据
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                System.out.println(new String(bytes, StandardCharsets.UTF_8));

                buffer.compact(); // 压缩缓冲区，保留未读数据
            }
        }
    }

    /**
     * 读取FileChannel内容
     *      也可以一次性将文件内容全部读取到内存
     */
    @Test
    public void test13() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(cwd, "src/test/resources/mmap.file");

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            long fileSize = ch.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);

            System.out.println("文件内容如下:");
            int bytesRead;
            if ((bytesRead = ch.read(buffer)) > 0) {
                buffer.flip(); // 切换到读模式
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                System.out.println(new String(data, StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * 读取FileChannel内容
     *      指定位置读取
     */
    @Test
    public void test21() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(cwd, "src/test/resources/mmap.file");

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            long fileSize = ch.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);

            ch.position(4);
            System.out.println("跳过前4个字节后,文件内容如下:");
            int bytesRead;
            if ((bytesRead = ch.read(buffer)) > 0) {
                buffer.flip(); // 切换到读模式
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                System.out.println(new String(data, StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * 读取FileChannel内容
     *      指定位置读取
     */
    @Test
    public void test22() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path path = Paths.get(cwd, "src/test/resources/mmap.file");

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            long fileSize = ch.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);

            System.out.println("跳过前4个字节后,文件内容如下:");
            int bytesRead;
            if ((bytesRead = ch.read(buffer, 4)) > 0) {
                buffer.flip(); // 切换到读模式
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                System.out.println(new String(data, StandardCharsets.UTF_8));
            }
        }
    }
}
