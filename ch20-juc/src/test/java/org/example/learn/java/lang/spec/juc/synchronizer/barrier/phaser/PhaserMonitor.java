package org.example.learn.java.lang.spec.juc.synchronizer.barrier.phaser;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

public class PhaserMonitor implements Runnable {

    final Phaser phaser;

    public PhaserMonitor(Phaser phaser) {
        this.phaser = phaser;
    }

    @Override
    public void run() {
        int checkSum = 0;

        while (!Thread.currentThread().isInterrupted()) {  // 中断结束线程工作

            if (checkSum != (checkSum = (phaser.getPhase() << 31) | (phaser.getRegisteredParties() << 16) | phaser.getUnarrivedParties())) {
                System.out.printf("phase=%d registeredParties=%d unarrivedParties=%d\n", phaser.getPhase(), phaser.getRegisteredParties(), phaser.getUnarrivedParties());
            }

            try {
                TimeUnit.MICROSECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // 将中断异常恢复为中断标识
            }
        }
    }
}
