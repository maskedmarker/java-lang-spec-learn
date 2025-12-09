package org.example.learn.java.lang.spec.juc.fork;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class SumTaskTest {

    class SumTask extends RecursiveTask<Long> {
        private final long[] arr;
        private final int start, end;
        private static final int THRESHOLD = 10000;

        public SumTask(long[] arr, int start, int end) {
            this.arr = arr;
            this.start = start;
            this.end = end;
        }

        /**
         * 如果当前线程是外部线程,那么外部线程都干了这些:切分任务->提交子任务->执行最小子任务->等待同级子任务完成->合并同级子任务->最终获得总任务结果并返回
         *
         * 当前线程也可以不做right子任务,将right子任务让fork-join线程池的其他线程来完成,当前线程就是等待子任务完成后合并. ⚠️这样坐等就是浪费cpu算力,同时增加线程上下文切换的开销.
         *
         * @return 计算结果
         */
        @Override
        protected Long compute() {
            // 定义最小子任务及其计算过程
            if (end - start <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) sum += arr[i];
                return sum;
            }

            // 将一个大任务切分为更小的子任务(小任务之间不能有依赖关系)
            int mid = (start + end) >>> 1;
            SumTask left = new SumTask(arr, start, mid);
            SumTask right = new SumTask(arr, mid, end);

            // 将left子任务交由fork-join线程池的线程来完成
            left.fork(); //📌开辟一条并行的计算链路

            // 当前线程完成right子任务
            long rightResult = right.compute(); // 📌继续执行当前的计算链路

            // right子任务已经完成,等待left子任务完成后,合并结果
            long leftResult = left.join(); // 📌合并并行计算链路的结果
            return leftResult + rightResult;
        }
    }

    @Test
    public void test0() {
        long[] arr = new long[10_000_000];
        Arrays.fill(arr, 1);

        ForkJoinPool pool = new ForkJoinPool();
        Long result = pool.invoke(new SumTask(arr, 0, arr.length));
        System.out.println("result = " + result);
    }
}
