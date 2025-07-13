package org.example.learn.java.lang.spec.juc.fork;

import org.junit.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * ForkJoinPool用于高效执行可分解的任务（通常是递归任务）且纯内存不涉及io/阻塞的.
 */
public class ForkJoinTaskTest {

    public class Fibonacci extends RecursiveTask<Integer> {
        final int n;
        Fibonacci(int n) { this.n = n; }

        @Override
        protected Integer compute() {
            if (n <= 1) {
                return n;
            }

            Fibonacci f1 = new Fibonacci(n - 1);
            // asynchronously execute this task in the pool the current task is running in, if applicable, or using the ForkJoinPool.commonPool() if not inForkJoinPool.
            f1.fork(); // 异步执行子任务
            Fibonacci f2 = new Fibonacci(n - 2);
            return f2.compute() + f1.join(); // 等待第一个子任务完成并获取结果
        }
    }

    @Test
    public void test0() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        Fibonacci task = new Fibonacci(10);
        System.out.println(pool.invoke(task));
    }
}
