package org.example.learn.java.lang.spec.io.file;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * mmap是一个非常重要的系统调用,用于内存映射文件或设备到进程的地址空间. 它提供了一种高效的文件I/O方式,避免了传统的read/write系统调用带来的数据拷贝开销.
 *
 * 将文件或设备映射到进程的虚拟内存空间;创建匿名的内存区域(不关联文件);
 *
 * FileChannel.map提供了mmap类似的接口
 * 
 * MappedByteBuffer相比ByteBuffer新增了 fore()、load() 和 isLoad()三个重要的方法：
 *      fore()：对于处于READ_WRITE模式下的缓冲区,把对缓冲区内容的修改强制刷新到本地文件.
 *      load()：将缓冲区的内容载入物理内存中,并返回这个缓冲区的引用.
 *      isLoaded()：如果缓冲区的内容在物理内存中,则返回 true,否则返回 false.
 * 
 */
public class MmapTest {


    @Test
    public void test01() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path mmapFilePath = Paths.get(cwd, "src/test/resources/mmap.file");

        // 不要使用: FileChannel fc = new FileInputStream(filePath).getChannel() 因为无法控制读写权限
        try (FileChannel ch = FileChannel.open(mmapFilePath, StandardOpenOption.READ)) {
            ByteBuffer buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0L, ch.size());
            Assert.assertTrue("", buffer.isDirect());
        }
    }

    /**
     * 读取文件数据
     */
    @Test
    public void test02() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path mmapFilePath = Paths.get(cwd, "src/test/resources/mmap.file");

        try (FileChannel ch = FileChannel.open(mmapFilePath, StandardOpenOption.READ)) {
            ByteBuffer buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0L, ch.size());
            System.out.println("buffer.position() = " + buffer.position());
            System.out.println("buffer.limit() = " + buffer.limit());
            System.out.println("buffer.remaining() = " + buffer.remaining());

            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * 将数据写入文件
     */
    @Test
    public void test03() throws IOException {
        String cwd = System.getProperty("user.dir");
        Path mmapFilePath = Paths.get(cwd, "src/test/resources/mmap2.file");
        String content = "你好";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        try (FileChannel ch = FileChannel.open(mmapFilePath, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            MappedByteBuffer mappedByteBuffer = ch.map(FileChannel.MapMode.READ_WRITE, 0L, bytes.length);
            // 写入数据
            mappedByteBuffer.put(bytes);
            // 将数据从内存刷入磁盘
            mappedByteBuffer.force();
        }
    }
}
