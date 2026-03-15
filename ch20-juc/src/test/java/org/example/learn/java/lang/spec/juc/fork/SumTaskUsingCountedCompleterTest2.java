package org.example.learn.java.lang.spec.juc.fork;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class SumTaskUsingCountedCompleterTest2 {

    static public class SumTask extends CountedCompleter<Void> {
        private static final int THRESHOLD = 1000; // 拆分阈值

        private final int[] array;
        private final int start;
        private final int end;
        private final AtomicLong totalSum; // 用于存储最终结果

        public SumTask(CountedCompleter<?> completer, int[] array, int start, int end, AtomicLong totalSum) {
            super(completer);
            this.array = array;
            this.start = start;
            this.end = end;
            this.totalSum = totalSum;
        }

        @Override
        public void compute() {
            int length = end - start;
            if (length <= THRESHOLD) {  // 到达阈值，直接计算
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                totalSum.addAndGet(sum);  // 将局部结果累加到共享结果中

                propagateCompletion();   // 尝试传播完成信号 //因为不需要onCompletion合并结果,所以可以用propagateCompletion代替tryComplete
            } else {
                int mid = (start + end) >>> 1;

                addToPendingCount(1);  // 设置待完成的子任务数量（本例中为1个右侧任务, 当前线程继续处理左侧子任务）
                new SumTask(this, array, mid, end, totalSum).fork(); // 启动右侧子任务

                new SumTask(this, array, start, mid, totalSum).compute();   // 节省线程切换开销
            }
        }
    }



    @Test
    public void test01() {
        int[] arr = new int[1_000_000];
        Arrays.fill(arr, 1);
        int expectedSum = IntStream.of(arr).sum();
        System.out.println("expectedSum = " + expectedSum);

        AtomicLong totalSum = new AtomicLong(0);
        ForkJoinPool pool = ForkJoinPool.commonPool();

        pool.invoke(new SumTask(null, arr, 0, arr.length, totalSum));    // invoke不仅提交任务,而且等到提交的任务执行完才能从invoke方法返回
        System.out.println("result = " + totalSum);
        Assert.assertEquals(expectedSum, totalSum.longValue());
    }

    @Test
    public void test02() {
        int[] arr = new int[1_000_000];
        Arrays.fill(arr, 1);
        int expectedSum = IntStream.of(arr).sum();
        System.out.println("expectedSum = " + expectedSum);

        AtomicLong totalSum = new AtomicLong(0);
        ForkJoinPool pool = ForkJoinPool.commonPool();

        ForkJoinTask<Void> submittedTask = pool.submit(new SumTask(null, arr, 0, arr.length, totalSum));// submit仅提交任务,返回值为入参任务(此处可以当作future使用)
        submittedTask.join();    // 等待任务完成
        System.out.println("result = " + totalSum);
        Assert.assertEquals(expectedSum, totalSum.longValue());
    }


    /**
     * 多次重复执行,降低误判的概率
     */
    @Test
    public void test03() {
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
