package org.example.learn.java.lang.spec.juc.synchronizer.barrier.phaser;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phaser的参与者
 *
 */
public class OneShotPhaserParty implements Runnable {

    final int threadId;
    final Phaser phaser;
    final long workTimeSec;

    public OneShotPhaserParty(Phaser phaser, long workTimeSec) {
        this.threadId = PhaserParty.THREAD_ID_GENERATOR.getAndIncrement();
        this.phaser = phaser;
        this.workTimeSec = workTimeSec;
    }

    public int getThreadId() {
        return threadId;
    }

    @Override
    public void run() {
        int phase = -1;
        phase = phaser.getPhase();
        System.out.printf("线程[%d]开始阶段[%d]的任务\n", threadId, phase);
        doBizWork();
        System.out.printf("线程[%d]完成阶段[%d]的任务,准备向phaser报备\n", threadId, phase);

        // arriveAndDeregister无视中断 💯💯💯
        phase = phaser.arriveAndDeregister();  // 通过调用arriveAndDeregister通知phaser当前参与者已完成本阶段任务且不再参与下个阶段,同时不等其他未完成任务的参与者

        System.out.printf("线程[%d]结束工作,最后完成的是阶段[%d]的任务\n", threadId, phase);
    }

    private void doBizWork() {
        try {
            Thread.sleep(workTimeSec * 1000L); // 模拟不同耗时的工作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // 将中断异常恢复为中断标识
        }
    }
}
