package org.example.learn.java.lang.spec.juc.synchronizer.barrier.phaser;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phaser的参与者
 *
 */
public class PhaserParty implements Runnable {

    private static final AtomicInteger THREAD_ID_GENERATOR = new AtomicInteger();

    final int threadId;
    final Phaser phaser;
    final boolean selfRegister;
    final long workTimeSec;
    final boolean repeatWork;
    int registerphase = -1;

    public PhaserParty(Phaser phaser, boolean selfRegister, long workTimeSec, boolean repeatWork) {
        this.threadId = THREAD_ID_GENERATOR.getAndIncrement();
        this.selfRegister = selfRegister;
        this.phaser = phaser;

        this.workTimeSec = workTimeSec;
        this.repeatWork = repeatWork;
    }

    public int getThreadId() {
        return threadId;
    }

    @Override
    public void run() {
        if (selfRegister) {
            // register无视中断 💯💯💯
            registerphase = phaser.register();// 注册参与者可能会发生阻塞(注册时阶段已完成,但是下个阶段迟迟不开始,当前线程只能等待)
            System.out.printf("线程[%d]注册到阶段[%d]\n", threadId, registerphase);
        }

        int phase = -1;
        do {
            phase = phaser.getPhase();
            System.out.printf("线程[%d]开始阶段[%d]的任务\n", threadId, phase);
            doBizWork();
            System.out.printf("线程[%d]完成阶段[%d]的任务,准备向phaser报备\n", threadId, phase);

            if (repeatWork) {
                // arriveAndAwaitAdvance无视中断 💯💯💯
                phase = phaser.arriveAndAwaitAdvance(); // 通过调用arrive通知phaser当前参与者已完成本阶段,(默认)还会去参与下个阶段,,现在等待其他未到达的线程(如果还参加下个阶段,此时本阶段还未结束,无法继续执行其他任务,只能等待)
            } else {
                // arrive无视中断 💯💯💯
                phase = phaser.arrive();  // 通过调用arrive通知phaser当前参与者已完成本阶段,(默认)还会去参与下个阶段,但是现在不去等待其他未到达的线程
            }
        } while (repeatWork && !Thread.currentThread().isInterrupted()); // 中断结束线程工作

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
