package org.example.learn.java.lang.spec.juc.fork;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class SumTaskUsingCountedCompleterTest2 {

    private static final int THRESHOLD = 10; // 拆分阈值

    static class SumTask extends CountedCompleter<Long> {
        final int[] array;
        final int start, end;  // [start,edn)闭开区间
        final AtomicLong localSum = new AtomicLong();  // 当前任务计算的求和值
        SumTask left, right;

        SumTask(CountedCompleter<?> parent, int[] array, int start, int end) {
            super(parent);
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        public void compute() {
            if (end - start <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++){
                    sum += array[i];
                }
                localSum.addAndGet(sum);
            } else {
                int mid = (start + end) >>> 1;
                setPendingCount(2);
                left = new SumTask(this, array, start, mid);
                right = new SumTask(this, array, mid, end);
                left.fork();
                right.fork();
            }

            tryComplete();
        }

        /**
         * 判断是否是叶子节点不能使用(caller != this)
         * onCompletion的入参caller可能是任务自己也可能是子任务:
         *      当父任务先执行了tryComplete时,caller就是任务自己
         *      当子任务先执行了tryComplete时,caller就是任务的子任务
         *  onCompletion的入参caller仅仅表示导致任务完成的最后一位贡献者(任务本体及其子任务都是贡献值)
         */
        @Override
        public void onCompletion(CountedCompleter<?> caller) {
            // 非叶子节点才可能需要合并子任务的结果
            if (left != null && right != null) {
                localSum.addAndGet(left.localSum.get());
                localSum.addAndGet(right.localSum.get());
            }
        }

        @Override
        public Long getRawResult() {
            return localSum.get();
        }
    }

    @Test
    public void test0() {
        int[] arr = new int[100];
        Arrays.fill(arr, 1);
        int expectedSum = IntStream.of(arr).sum();
        System.out.println("expectedSum = " + expectedSum);

        ForkJoinPool pool = ForkJoinPool.commonPool();
        SumTask rootTask = new SumTask(null, arr, 0, arr.length);
        Long result = pool.invoke(rootTask);
        System.out.println("result = " + result);
        Assert.assertEquals(expectedSum, result.longValue());
    }

    @Test
    public void test01() {
        int[] arr = new int[10000];
        Arrays.fill(arr, 1);
        int expectedSum = IntStream.of(arr).sum();

        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (int i = 0; i < 1000; i++) {
            System.out.printf("------------------counter=%d-------------------------\n", i);
            doTest(pool, arr, expectedSum);
        }
    }

    public void doTest(ForkJoinPool pool, int[] arr, int expectedSum) {
        SumTask rootTask = new SumTask(null, arr, 0, arr.length);
        Long result = pool.invoke(rootTask);
        if (expectedSum != result) {
            System.out.printf("expectedSum=%d, result = %d\n", expectedSum, result);
            System.exit(-1);
        }
    }

    @Test
    public void test1() {
        int lo = 0;
        int hi = 100;

        do{
            int mid = (lo + hi) >>> 1;
            System.out.printf("[%d %d) [%d %d)\n", lo, mid, mid, hi);

            hi = mid;
        } while(hi - lo > THRESHOLD);
    }
}
