package org.example.learn.java.lang.spec.juc.synchronizer.phaser;

import org.example.learn.java.lang.spec.juc.util.ThreadUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PhaserTest2 {
    
    private static final int NUMBER_OF_WORKER = 3;


    @Test
    public void test01() {
        Phaser phaser = new Phaser(NUMBER_OF_WORKER);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Thread monitorThread;
        (monitorThread = new Thread(new PhaserMonitor(phaser))).start();
        int phase = phaser.getPhase();

        Thread[] workers = new Thread[NUMBER_OF_WORKER];
        for (int i = 0; i < NUMBER_OF_WORKER - 1; i++) {
            (workers[i] = new Thread(new PhaserParty(phaser, false, random.nextInt(0, 4), true))).start();  // repeat-work
        }
        (workers[NUMBER_OF_WORKER - 1] = new Thread(new TroublePhaserParty(phaser, false, random.nextInt(0, 1), true))).start();  // repeat-work

        int maxWaitSec = 4 * NUMBER_OF_WORKER;
        int waitPhase = 0;
        try {
            waitPhase = phaser.awaitAdvanceInterruptibly(phase, maxWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            Assert.assertTrue("有参与者无法推进阶段完成,导致等待超时", e instanceof TimeoutException);
            Assert.assertEquals("有参与者无法推进阶段完成", phase, waitPhase);
            Assert.assertEquals("有参与者无法推进阶段完成", 1, phaser.getUnarrivedParties());
        }

        long waitingCount = Arrays.stream(workers).filter(i -> i.getState().equals(Thread.State.WAITING)).count();
        System.out.println("waitingCount = " + waitingCount);
        Assert.assertEquals("有参与者无法推进阶段完成,正常线程还处于等待状态", NUMBER_OF_WORKER - 1, waitingCount);


        System.out.println("用户代码需要自己兜底异常,并释放哪些正常等待的线程");
        System.out.println("兜底异常,要么补充新的参与者,要么终止phaser");
        // 选择终止
        phaser.forceTermination();
        ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
        waitingCount = Arrays.stream(workers).filter(i -> i.getState().equals(Thread.State.WAITING)).count();
        System.out.println("waitingCount = " + waitingCount);
        Assert.assertEquals("用户代码需要自己兜底异常,并释放哪些正常等待的线程", 0, waitingCount);

        monitorThread.interrupt();
        // 用户自己清理那些无限循环的参与者(这里省略)
    }

    @Test
    public void test02() {
        Phaser phaser = new Phaser(NUMBER_OF_WORKER);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Thread monitorThread;
        (monitorThread = new Thread(new PhaserMonitor(phaser))).start();
        int phase = phaser.getPhase();

        Thread[] workers = new Thread[NUMBER_OF_WORKER];
        for (int i = 0; i < NUMBER_OF_WORKER - 1; i++) {
            (workers[i] = new Thread(new PhaserParty(phaser, false, random.nextInt(0, 4), true))).start();  // 无限循环的参与者
        }
        (workers[NUMBER_OF_WORKER - 1] = new Thread(new TroublePhaserParty(phaser, false, 0, true))).start();  // 无限循环的参与者

        int maxWaitSec = 4 * NUMBER_OF_WORKER;
        int waitPhase = 0;
        try {
            waitPhase = phaser.awaitAdvanceInterruptibly(phase, maxWaitSec, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            Assert.assertTrue("有参与者无法推进阶段完成,导致等待超时", e instanceof TimeoutException);
            Assert.assertEquals("有参与者无法推进阶段完成", phase, waitPhase);
            Assert.assertEquals("有参与者无法推进阶段完成", 1, phaser.getUnarrivedParties());
        }

        long waitingCount = Arrays.stream(workers).filter(i -> i.getState().equals(Thread.State.WAITING)).count();
        System.out.println("waitingCount = " + waitingCount);
        Assert.assertEquals("有参与者无法推进阶段完成,正常线程还处于等待状态", NUMBER_OF_WORKER - 1, waitingCount);


        System.out.println("用户代码需要自己兜底异常,并释放哪些正常等待的线程");
        System.out.println("兜底异常,要么补充新的参与者,要么终止phaser");

        // 选择补充新的参与者
        new Thread(new PhaserParty(phaser, false, 0, true)).start();
        while (phaser.getPhase() == phase) {
            ThreadUtils.yieldWait(1, TimeUnit.SECONDS);
        }
        waitingCount = Arrays.stream(workers).filter(i -> i.getState().equals(Thread.State.WAITING)).count();
        System.out.println("waitingCount = " + waitingCount);
        Assert.assertEquals("用户代码需要自己兜底异常,并释放哪些正常等待的线程", 0, waitingCount);

        monitorThread.interrupt();
        // 用户自己清理那些无限循环的参与者(这里省略)
    }
}
