package org.example.learn.java.lang.spec.juc.synchronizer.exchanger;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;


/**
 * Exchanger不仅适用于固定的2个线程一次或多次的交换,
 * 还适用于不固定的多个(偶数)线程一次或多次的交换
 */
public class ExchangerTest {

    static class RecurringParticipant implements Runnable{

        private final String name;
        private final Exchanger<String> exchanger;
        private final int recurNum;
        private final String data;

        RecurringParticipant(String name, Exchanger<String> exchanger, String data, int recurNum) {
            this.name = name;
            this.exchanger = exchanger;
            this.data = data;
            this.recurNum = recurNum;
        }

        @Override
        public void run() {
            try {
                for (int j = 0; j < recurNum; j++) {
                    String exchangeData = data + "-" + j;
                    System.out.printf("线程%s: 第%d次发送[%s]\n", name, j, exchangeData);
                    String received = exchanger.exchange(exchangeData);
                    System.out.printf("线程%s: 第%d次收到[%s]\n", name, j, received);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 2个线程 一对一交换一次
     */
    @Test
    public void test01() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();

        Thread threadA = new Thread(new RecurringParticipant("线程A", exchanger, "Hello", 1));
        Thread threadB = new Thread(new RecurringParticipant("线程B", exchanger, "World", 1));
        threadA.start();
        threadB.start();

        // 等待工作线程自然结束,防止junit在测试方法结束后主动杀死工作线程
        threadA.join();
        threadB.join();
    }

    /**
     * 2个线程 一对一交换多次
     */
    @Test
    public void test02() throws InterruptedException {
        final Exchanger<String> exchanger = new Exchanger<>();
        final int exchangeNum = 3;

        // 生产者线程
        Thread producer = new Thread(new RecurringParticipant("生产者", exchanger, "生产者的数据", exchangeNum));

        // 消费者线程
        Thread consumer = new Thread(new RecurringParticipant("消费者", exchanger, "消费者的数据", exchangeNum));

        producer.start();
        consumer.start();

        // 等待工作线程自然结束,防止junit在测试方法结束后主动杀死工作线程
        producer.join();
        consumer.join();
    }

    /**
     * 多个线程 多对多交换一次
     */
    @Test
    public void test11() throws InterruptedException {
        final Exchanger<String> exchanger = new Exchanger<>();
        final int participantNum = 8 & (~1); // 必须是偶数,否者无法两两配对交换

        List<Thread> workers = new ArrayList<>(participantNum);
        // 创建多个线程，两两配对交换
        for (int i = 0; i < participantNum; i++) {
            Thread worker = new Thread(new RecurringParticipant("线程" + i, exchanger, "" + i, 1));;
            workers.add(worker);
        }

        // 减小启动间隙,增加不确定性
        workers.forEach(Thread::start);


        // 等待工作线程自然结束,防止junit在测试方法结束后主动杀死工作线程
        for (Thread worker : workers) {
            worker.join();
        }
    }

    /**
     * 多个线程 多对多交换多次
     */
    @Test
    public void test12() throws InterruptedException {
        final Exchanger<String> exchanger = new Exchanger<>();
        final int participantNum = 8 & (~1); // 必须是偶数,否者无法两两配对交换
        final int exchangeNum = 3;

        List<Thread> workers = new ArrayList<>(participantNum);
        // 创建多个线程，两两配对交换
        for (int i = 0; i < participantNum; i++) {
            Thread worker = new Thread(new RecurringParticipant("线程" + i, exchanger, "" + i, exchangeNum));
            workers.add(worker);
        }

        // 减小启动间隙,增加不确定性
        workers.forEach(Thread::start);


        // 等待工作线程自然结束,防止junit在测试方法结束后主动杀死工作线程
        for (Thread worker : workers) {
            worker.join();
        }
    }

    /**
     * 多个线程 多对多交换一次 动态补充
     */
    @Test
    public void test13() throws InterruptedException {
        final Exchanger<String> exchanger = new Exchanger<>();
        final int participantNum = 8 | 1 ; // 创造奇数,导致无法两两配对交换

        List<Thread> workers = new ArrayList<>(participantNum);
        // 创建多个线程，两两配对交换
        for (int i = 0; i < participantNum; i++) {
            Thread worker = new Thread(new RecurringParticipant("线程" + i, exchanger, "" + i, 1));
            workers.add(worker);
        }

        // 减小启动间隙,增加不确定性
        workers.forEach(Thread::start);


        // 由于有个线程无法匹配,导致主线程等待超时
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(2));
        }
        long waitingCount = workers.stream().filter(t -> t.getState().equals(Thread.State.WAITING)).count();
        System.out.println("waitingCount = " + waitingCount);
        Assert.assertEquals(1, waitingCount);

        // 补充一个参与者
        Thread candidate = new Thread(new RecurringParticipant("线程" + participantNum, exchanger, "" + participantNum, 1));
        workers.add(candidate);
        candidate.start();


        // 等待工作线程自然结束,防止junit在测试方法结束后主动杀死工作线程
        for (Thread worker : workers) {
            worker.join();
        }
    }
}
