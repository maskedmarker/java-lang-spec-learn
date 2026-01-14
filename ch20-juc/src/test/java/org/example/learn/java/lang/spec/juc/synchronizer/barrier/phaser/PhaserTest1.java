package org.example.learn.java.lang.spec.juc.synchronizer.barrier.phaser;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * arrive()方法是非阻塞的
 */
public class PhaserTest1 {
    
    private static final int NUMBER_OF_WORKER = 3;

    /**
     * 当所有参与者完成某个阶段,该阶段才会推进
     */
    @Test
    public void test01() throws InterruptedException {
        // 屏障的初始阶段的参与者为3
        Phaser phaser = new Phaser(3);

        Thread monitorThread;
        (monitorThread = new Thread(new PhaserMonitor(phaser))).start();


        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            new Thread(new PhaserParty(phaser, false, i, false)).start();
        }

        // 当所有参与者完成初始阶段,阶段才会推进到1
        while (phaser.getPhase() == 0) {
            TimeUnit.SECONDS.sleep(1);
        }
        Assert.assertEquals("当所有参与者完成初始阶段,阶段才会自增加一", 1, phaser.getPhase());

        System.out.println("all worker has done job !!!");
        monitorThread.interrupt();
    }

    /**
     * 当所有参与者完成某个阶段,该阶段才会推进.
     * awaitAdvance可以被非参与者线程使用
     */
    @Test
    public void test02() throws InterruptedException {
        // 屏障的初始阶段的参与者为3
        Phaser phaser = new Phaser(3);

        Thread monitorThread;
        (monitorThread = new Thread(new PhaserMonitor(phaser))).start();


        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            new Thread(new PhaserParty(phaser, false, i, false)).start();
        }

        // 当所有参与者完成初始阶段,阶段才会推进到1
        while (phaser.getPhase() == 0) {
            phaser.awaitAdvance(phaser.getPhase());
        }
        Assert.assertEquals("当所有参与者完成初始阶段,阶段才会自增加一", 1, phaser.getPhase());

        System.out.println("all worker has done job !!!");
        monitorThread.interrupt();
    }


    /**
     * Phaser是可以多次重复使用的,前提是phaser不能终结
     * 当某个阶段结束时,所有参与者都不再参与下个阶段时,此时phaser就会发生终结.终结后就不能再使用了
     */
    @Test
    public void test11() throws InterruptedException {
        // 屏障的初始阶段的参与者为3
        Phaser phaser = new Phaser(3);

        Thread monitorThread;
        (monitorThread = new Thread(new PhaserMonitor(phaser))).start();


        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            new Thread(new PhaserParty(phaser, false, i, false)).start();
        }

        // 当所有参与者完成初始阶段,阶段才会自增加一
        while (phaser.getPhase() == 0) {
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.printf("phase=%d registeredParties=%d unarrivedParties=%d\n", phaser.getPhase(), phaser.getRegisteredParties(), phaser.getUnarrivedParties());
        Assert.assertEquals("当所有参与者完成初始阶段,阶段才会自增加一", 1, phaser.getPhase());

        System.out.println("all workers1 have done job !!!");

        System.out.println("---------------------------------------------------------------------------");

        // 上个阶段的registeredParties和unarrivedParties不会清空,新的register会同时增加registeredParties和unarrivedParties
        // 没有deregister的单独方法,想要deregister,只有arriveAndDeregister
        int registerPhase = phaser.bulkRegister(NUMBER_OF_WORKER); // add-delta
        Assert.assertTrue("当某个阶段结束时,所有参与者都不再参与下个阶段时,此时phaser就会发生终结.终结后就不能再使用了", registerPhase >= 0);
        System.out.printf("phase=%d registeredParties=%d unarrivedParties=%d\n", phaser.getPhase(), phaser.getRegisteredParties(), phaser.getUnarrivedParties());
        for (int i = 0; i < (NUMBER_OF_WORKER * 2); i++) {
            new Thread(new PhaserParty(phaser, false, i, false)).start();
        }

        while (phaser.getPhase() == 1) {
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.printf("phase=%d registeredParties=%d unarrivedParties=%d\n", phaser.getPhase(), phaser.getRegisteredParties(), phaser.getUnarrivedParties());
        Assert.assertEquals("当所有参与者完成初始阶段,阶段才会自增加一", 2, phaser.getPhase());

        System.out.println("all workers2 have done job !!!");
    }

    /**
     * Phaser是可以多次重复使用的,前提是phaser不能终结
     * 当某个阶段结束时,所有参与者都不再参与下个阶段时,此时phaser就会发生终结.终结后就不能再使用了
     */
    @Test
    public void test12() throws InterruptedException {
        // 屏障的初始阶段的参与者为3
        Phaser phaser = new Phaser(3);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Thread monitorThread;
        (monitorThread = new Thread(new PhaserMonitor(phaser))).start();


        for (int i = 0; i < NUMBER_OF_WORKER; i++) {
            new Thread(new OneShotPhaserParty(phaser,  random.nextInt(1, 5))).start();
        }
        // 当所有参与者完成初始阶段,阶段才会自增加一
        while (phaser.getPhase() == 0) {
            TimeUnit.SECONDS.sleep(1);
        }
        Assert.assertEquals("当所有参与者完成初始阶段,阶段才会自增加一", 1, phaser.getPhase() & Integer.MAX_VALUE);  // 当某个阶段结束时,所有参与者都不再参与下个阶段时,此时phaser就会发生终结.终结后就不能再使用了

        System.out.println("all workers1 have done job !!!");

        System.out.println("-------------------------------------------------------------------------------------------------------------------");

        // 因为使用arriveAndDeregister,上个阶段的registeredParties和unarrivedParties会清空
        int registeredPhase = phaser.bulkRegister(NUMBER_OF_WORKER); // add-delta
        Assert.assertTrue("当某个阶段结束时,所有参与者都不再参与下个阶段时,此时phaser就会发生终结.终结后就不能再使用了", registeredPhase < 0);
        Assert.assertTrue("当某个阶段结束时,所有参与者都不再参与下个阶段时,此时phaser就会发生终结.终结后就不能再使用了", phaser.isTerminated());

        System.out.println("all workers2 have done job !!!");
    }

    /**
     * 动态注册参与者
     *    如果让工作线程自主注册,则他们注册到的阶段会出现不同.
     *    如果想避免这种情况发生,可以由管理线程提前统一注册,即bulkRegister
     */
    @Test
    public void test21() throws InterruptedException {
        // 屏障初始阶段的开始先不注册参与者
        Phaser phaser = new Phaser();

        // phaser monitor
        Thread monitorThread;
        (monitorThread = new Thread(new PhaserMonitor(phaser))).start();


        // party thread
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < NUMBER_OF_WORKER << 3; i++) {
            TimeUnit.SECONDS.sleep(random.nextInt(0, 2));
            PhaserParty party = new PhaserParty(phaser, true, random.nextInt(1, 3), true);  // 动态注册参与者
            System.out.printf("thread[%s]完成注册\n", party.getThreadId());
            new Thread(party).start();
        }


        while (phaser.getPhase() <= 1) {
            TimeUnit.SECONDS.sleep(1);
        }
        monitorThread.interrupt();

        // 注意观察控制台输出的内容: 注册到阶段

        // 用户自己清理那些无限循环的参与者(这里省略)
        // junit会在本方法执行完后,销毁工作线程
    }
}
