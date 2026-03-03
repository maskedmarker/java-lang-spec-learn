package org.example.learn.java.lang.spec.juc.synchronizer;

import org.example.learn.java.lang.spec.juc.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * 不同于Exchanger可以支持多个reader/writer公用同一个exchanger对象(Exchanger保证多reader/writer并发安全),
 * Pipe应该只有一对reader/writer使用同一个pipe对象(Pipe不保证多reader/writer并发安全)
 */
public class PipeTest {

    private static class ReadWorker implements Runnable {

        private Pipe.SourceChannel sourceChannel;

        public ReadWorker(Pipe.SourceChannel sourceChannel) {
            this.sourceChannel = sourceChannel;
        }

        @Override
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            while (!Thread.currentThread().isInterrupted()) {
                buffer.clear();

                try {
                    int read = sourceChannel.read(buffer);
                    buffer.flip();

                    if (read > 0) {
                        int i = buffer.getInt();
                        System.out.println("read: " + i);

                        if (i == 0) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("ReadWorker thread terminates");
        }
    }

    private static class WriteWorker implements Runnable {

        private Pipe.SinkChannel sinkChannel;

        public WriteWorker(Pipe.SinkChannel sinkChannel) {
            this.sinkChannel = sinkChannel;
        }

        @Override
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            int counter = 1;
            int times = 100;
            while (!Thread.currentThread().isInterrupted()) {
                buffer.clear();
                buffer.putInt(counter++).flip();

                try {
                    sinkChannel.write(buffer);
                    System.out.println("write: " + (counter - 1));

                    // 最多执行100次,然后结束
                    if (counter > times) {
                        buffer.clear();
                        buffer.putInt(0).flip();
                        sinkChannel.write(buffer);
                        System.out.println("write: " + 0);
                        break;
                    }

                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("WriteWorker thread terminates");
        }
    }


    @Test
    public void test0() throws Exception {
        Pipe pipe = Pipe.open();
        Thread writerThread = new Thread(new WriteWorker(pipe.sink()));
        Thread readerThread = new Thread(new ReadWorker(pipe.source()));

        readerThread.start();
        writerThread.start();

        writerThread.join();
        readerThread.join();
    }

    /**
     * Pipe在本地开了2个端口,sink/source是2个AbstractSelectableChannel,与nio的client/server对应
     */
    @Test
    public void test1() throws Exception {
        Pipe pipe = Pipe.open();
        Object source = ReflectionUtils.getFieldValue(pipe, "source");
        Object sink = ReflectionUtils.getFieldValue(pipe, "sink");
        Assert.assertNotNull(source);
        Assert.assertNotNull(sink);
        System.out.println("source.getClass() = " + source.getClass());
        System.out.println("sink.getClass() = " + sink.getClass());
        Assert.assertTrue(source instanceof Pipe.SourceChannel);
        Assert.assertTrue(sink instanceof Pipe.SinkChannel);


        SocketChannel sourceSocketChannel = ReflectionUtils.getFieldValue(source, "sc");
        SocketChannel sinkSocketChannel = ReflectionUtils.getFieldValue(sink, "sc");
        System.out.println("sourceSocketChannel = " + sourceSocketChannel);
        System.out.println("sinkSocketChannel = " + sinkSocketChannel);
    }
}
