package org.example.learn.java.lang.spec.io.net.socket;

import org.example.learn.java.lang.spec.io.net.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SelectorTest {

    /**
     *  On all platforms, Selector must support:
     *      selector.wakeup()
     *      thread-safe register() / cancel() while another thread is blocked in select()
     *
     *  In sun.nio.ch.WindowsSelectorImpl, a Pipe is used to wake up and control a selector that is blocked in a native select() call.
     *  This design compensates for limitations of the Windows I/O multiplexing model.
     */
    @Test
    public void test0() throws IOException {
        Selector selector = Selector.open();
        System.out.println("selector.getClass() = " + selector.getClass());

        Object wakeupPipe = ReflectionUtils.getFieldValue(selector, "wakeupPipe");
        Assert.assertNotNull(wakeupPipe);
        Assert.assertTrue(wakeupPipe instanceof Pipe);

        Object source = ReflectionUtils.getFieldValue(wakeupPipe, "source");
        Object sink = ReflectionUtils.getFieldValue(wakeupPipe, "sink");
        Assert.assertNotNull(source);
        Assert.assertNotNull(sink);
        Assert.assertTrue(source instanceof Pipe.SourceChannel);
        Assert.assertTrue(sink instanceof Pipe.SinkChannel);

        System.out.println("source.getClass() = " + source.getClass());
        System.out.println("sink.getClass() = " + sink.getClass());

        SocketChannel sourceSocketChannel = ReflectionUtils.getFieldValue(source, "sc");
        SocketChannel sinkSocketChannel = ReflectionUtils.getFieldValue(sink, "sc");

        System.out.println("sourceSocketChannel = " + sourceSocketChannel);
        System.out.println("sinkSocketChannel = " + sinkSocketChannel);
    }
}
