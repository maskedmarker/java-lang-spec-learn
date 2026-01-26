package org.example.learn.java.lang.spec.io.net.buffer;

import org.example.learn.java.lang.spec.io.net.util.BufferUtils;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * A buffer is a linear, finite sequence of elements of a specific primitive type.
 */
public class ByteBufferTest {


    /**
     * 创建ByteBuffer的几种方式
     */
    @Test
    public void test01() {
        // 分配堆内存
        ByteBuffer heapByteBuffer = ByteBuffer.allocate(16);

        // 包装现有数组(使用的也是堆内存)
        byte[] byteArray = new byte[16];
        ByteBuffer heapByteBuffer2 = ByteBuffer.wrap(byteArray);

        // 分配直接内存(堆外内存)
        ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(16);
    }

    /**
     * ByteBuffer的基本读写操作
     */
    @Test
    public void test02() {
        // 新创建后,就处于写模式
        ByteBuffer buffer = ByteBuffer.allocate(16);

        // 初始状态
        BufferUtils.logBufferStat("初始状态", buffer);

        // 写入数据
        buffer.put((byte) 1);
        BufferUtils.logBufferStat("写入1个字节后", buffer);
        buffer.put((byte) 2);
        BufferUtils.logBufferStat("写入2个字节后", buffer);
        buffer.put((byte) 3);
        BufferUtils.logBufferStat("写入3个字节后", buffer);

        // 切换为读模式
        buffer.flip();
        BufferUtils.logBufferStat("flip()后", buffer);

        // 读取数据
        System.out.println("读取第一个字节: " + buffer.get());
        BufferUtils.logBufferStat("读取第一个字节后", buffer);
        System.out.println("读取第二个字节: " + buffer.get());
        BufferUtils.logBufferStat("读取第二个字节后", buffer);

        // 仅重置position
        buffer.rewind();
        BufferUtils.logBufferStat("rewind()后", buffer);

        // 清空缓冲区（重置position/limit/mark）
        buffer.clear();
        BufferUtils.logBufferStat("clear()后", buffer);
    }

    /**
     * ByteBuffer的position操作
     */
    @Test
    public void test11() {
        // 新创建后,就处于写模式
        ByteBuffer buffer = ByteBuffer.allocate(8);
        // 填充数据
        for (int i = 0; i < 8; i++) {
            buffer.put((byte) (i + 1));
        }
        // 刚才往byteBuffer写入了数据,如果要读取就要切换为读模式
        buffer.flip();
        BufferUtils.logBufferStat("写入8个字节,flip()后", buffer);

        // mark() 和 reset()
        buffer.get();  // 读取1
        buffer.get();  // 读取2
        buffer.mark(); // 标记当前position
        BufferUtils.logBufferStat("读取2个字节,mark()后", buffer);
        buffer.get();  // 读取3
        buffer.get();  // 读取4
        BufferUtils.logBufferStat("再读取2个字节后", buffer);

        // Resets this buffer's position to the previously-marked position.
        buffer.reset();
        BufferUtils.logBufferStat("reset()后", buffer);
    }

    /**
     * ByteBuffer的position操作
     *
     *  compact() 将未操作的数据移动到数组最前面,position设置为下个当前待操作的index上
     *  前面是读模式,现在需要切换到写模式且希望保留未读取的数据,就可以使用compact()
     */
    @Test
    public void test121() {
        // 新创建后,就处于写模式
        ByteBuffer buffer2 = ByteBuffer.allocate(10);
        for (int i = 0; i < 8; i++) {
            buffer2.put((byte) i);
        }
        // 切换为读模式
        buffer2.flip();
        BufferUtils.logBufferStat("写入8个字节,flip()后", buffer2);

        buffer2.get();  // 读取0
        buffer2.get();  // 读取1
        BufferUtils.logBufferStat("读取2个字节后,compact()前", buffer2);

        // compact带有切换模式的含义,💯💯💯
        // 如果之前读操作还有未读取的数据,compact后不用flip而可以直接接着写数据,同时保留未读取的数据; 如果之前是写操作,compact操作毫无意义
        buffer2.compact();
        BufferUtils.logBufferStat("读取2个字节后,compact()后", buffer2);


        // 前面的compact导致切换到写模式,此时要读取剩余数据,还需要切换回读模式
        buffer2.flip();
        System.out.print("剩余数据: ");
        while (buffer2.hasRemaining()) {
            System.out.print(buffer2.get() + " ");
        }
    }

    /**
     * ByteBuffer的position操作
     *
     * compact()的错误使用方式
     */
    @Test
    public void test122() {
        // compact() 压缩缓冲区
        ByteBuffer buffer2 = ByteBuffer.allocate(10);
        for (int i = 0; i < 8; i++) {
            buffer2.put((byte) i);
        }
        // 写入数据后,准备被读取
        buffer2.flip();
        BufferUtils.logBufferStat("写入8个字节,flip()后", buffer2);

        buffer2.get();  // 读取0
        buffer2.get();  // 读取1
        BufferUtils.logBufferStat("读取2个字节后,compact()前", buffer2);
        buffer2.compact();
        BufferUtils.logBufferStat("读取2个字节后,compact()后", buffer2);

//        buffer2.flip();
        // 如果没有flip切换到读模式,此时hasRemaining()返回是否还能写的判断而非是否还能读,而且get()返回的都是无意义的数据
        System.out.print("剩余数据: ");
        while (buffer2.hasRemaining()) {
            System.out.print(buffer2.get() + " ");
        }
    }
}
