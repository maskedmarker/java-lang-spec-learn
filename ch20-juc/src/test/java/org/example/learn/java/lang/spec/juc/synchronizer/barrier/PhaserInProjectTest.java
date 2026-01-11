package org.example.learn.java.lang.spec.juc.synchronizer.barrier;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;

/**
  Phaser在真实工程中的实践

  一、真实问题背景（工程语境）
    场景：并行拉取 → 处理 → 汇总的批处理任务
     你在做一个数据聚合服务，流程如下：
         从多个第三方 API 拉取数据（Source 数量运行期才能确定）
         每个 Source 内部又拆分成多个并行子任务
         每一轮拉取完成后，进行一次 全局聚合
         聚合完成后进入下一轮（可能是重试、补偿、增量）
         当没有活跃 Source 时，流程自动结束
 */
public class PhaserInProjectTest {

    static class BatchPhaser extends Phaser {

        BatchPhaser(int parties) {
            super(parties);
        }

        // onAdvance 是 Phaser 的“生命周期钩子”,返回true则Phaser终止,不需要额外shutdown 逻辑
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            // 每个 phase 结束时都会回调（在锁外）
            logPhase(phase, registeredParties);

            // 没有活跃任务，终止整个流程
            return registeredParties == 0;
        }

        private void logPhase(int phase, int parties) {
            System.out.printf(
                    "Phase %d completed, active parties=%d%n",
                    phase, parties
            );
        }
    }

    static class Job {

        public void process() {
            System.out.println("job processing");
        }
    }

    static class Source {

        public List<Job> fetchJobs() {
            return new ArrayList<>();
        }
    }

    static class SourceTask implements Runnable {

        private final Source source;
        private final BatchPhaser phaser;

        SourceTask(Source source, BatchPhaser phaser) {
            this.source = source;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            try {
                while (!phaser.isTerminated()) {

                    List<Job> jobs = source.fetchJobs();
                    if (jobs.isEmpty()) {
                        return;
                    }

                    // 子任务动态注册
                    for (Job job : jobs) {
                        phaser.register();
                        processAsync(job);
                    }

                    // Source 自身阶段完成
                    phaser.arriveAndAwaitAdvance();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                phaser.arriveAndDeregister();
            }
        }

        private void processAsync(Job job) {
            CompletableFuture.runAsync(() -> {
                try {
                    job.process();
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
    }


    @Test
    public void test0() throws InterruptedException {

    }



}
