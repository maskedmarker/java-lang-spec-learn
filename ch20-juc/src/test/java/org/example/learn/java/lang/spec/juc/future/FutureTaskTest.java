package org.example.learn.java.lang.spec.juc.future;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class FutureTaskTest {

    @Test
    public void test0() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> {
            TimeUnit.SECONDS.sleep(3);
            return "done";
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);

        while (!task.isDone()) {
            System.out.println("task还未完成");
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("task已完成");
    }
}
