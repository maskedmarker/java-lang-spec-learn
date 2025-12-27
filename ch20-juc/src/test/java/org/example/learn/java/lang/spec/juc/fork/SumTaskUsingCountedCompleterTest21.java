package org.example.learn.java.lang.spec.juc.fork;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * SumTaskUsingCountedCompleterTest2的改进版
 */
public class SumTaskUsingCountedCompleterTest21 {

    static public class SumTask extends CountedCompleter<Void> {
        private static final int THRESHOLD = 1000; // 拆分阈值

        private final int[] array;
        final int start; int end;   // [start,edn)闭开区间
        private final AtomicLong totalSum; // 用于存储最终结果

        public SumTask(CountedCompleter<?> completer, int[] array, int start, int end, AtomicLong totalSum) {
            super(completer);
            this.array = array;
            this.start = start;
            this.end = end;
            this.totalSum = totalSum;
        }

        /**
         * SumTaskUsingCountedCompleterTest2.compute的相同逻辑的另一种写法.
         * 当前版本直接并没有消除的左右子任务的概念,只是更隐蔽.
         */
        @Override
        public void compute() {
            while((end - start) > THRESHOLD) {
                int mid = (start + end) >>> 1;
                addToPendingCount(1);
                new SumTask(this, array, mid, end, totalSum).fork();
                end = mid;
            }

            long sum = 0;
            if((end - start) <= THRESHOLD) {
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
            }
            totalSum.addAndGet(sum);
            propagateCompletion(); // 因为totalSum的引入,不需要onCompletion来合并结果,所以可以使用propagateCompletion代替tryComplete
        }
    }



    @Test
    public void test0() {
        int[] arr = new int[1_000_000];
        Arrays.fill(arr, 1);
        int expectedSum = IntStream.of(arr).sum();
        System.out.println("expectedSum = " + expectedSum);

        AtomicLong totalSum = new AtomicLong(0);
        ForkJoinPool pool = ForkJoinPool.commonPool();

        pool.invoke(new SumTask(null, arr, 0, arr.length, totalSum));
        System.out.println("result = " + totalSum);
        Assert.assertEquals(expectedSum, totalSum.longValue());
    }


    /**
     * 多次重复执行,降低误判的概率
     */
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
        AtomicLong totalSum = new AtomicLong(0);
        SumTask rootTask = new SumTask(null, arr, 0, arr.length, totalSum);
        pool.invoke(rootTask);
        if (expectedSum != totalSum.get()) {
            System.out.printf("expectedSum=%d, totalSum = %d\n", expectedSum, totalSum.get());
            System.exit(-1);
        }
    }

    @Test
    public void test1() {
        int lo = 0;
        int hi = 10_000_000;

        do{
            int mid = (lo + hi) >>> 1;
            System.out.printf("[%d %d) [%d %d)\n", lo, mid, mid, hi);

            hi = mid;
        } while(hi - lo > 1000);
    }
}
