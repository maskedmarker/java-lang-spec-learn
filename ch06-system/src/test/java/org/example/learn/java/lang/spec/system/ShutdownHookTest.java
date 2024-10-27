package org.example.learn.java.lang.spec.system;

import org.junit.Test;

import java.io.IOException;

/**
 * javadoc中这样描述
 * The Java virtual machine shuts down in response to two kinds of events:
 * The program exits normally, when the last non-daemon thread exits or when the exit (equivalently, System.exit) method is invoked, or
 * The virtual machine is terminated in response to a user interrupt, such as typing ^C, or a system-wide event, such as user logoff or system shutdown.
 *
 * ShutdownHook可以在正常和非正常中执行
 */
public class ShutdownHookTest {

    /**
     * ShutdownHook
     */
    @Test
    public void test0() {
        Thread hook = new Thread(() -> {
            System.out.println("running in shutdown hook");
        });

        Runtime.getRuntime().addShutdownHook(hook);

        // 阻塞当前线程,然后手动kill jvm进程
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
