package org.example.learn.java.lang.spec.juc.condition;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumerExample {

    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 5;

    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();  // 队列未满条件
    private final Condition notEmpty = lock.newCondition(); // 队列非空条件

    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            lock.lock();
            try {
                // 如果队列已满，等待
                while (queue.size() == capacity) {
                    System.out.println("队列已满，生产者等待...");
                    notFull.await(); // 注意: await支持中断,所以要在finally中unlock
                }

                System.out.println("生产者生产: " + value);
                queue.offer(value++);

                // 通知消费者可以消费了
                notEmpty.signal();

                Thread.sleep(500); // 模拟生产过程
            } finally {
                lock.unlock();
            }
        }
    }

    public void consume() throws InterruptedException {
        while (true) {
            lock.lock();
            try {
                // 如果队列为空，等待
                while (queue.isEmpty()) {
                    System.out.println("队列为空，消费者等待...");
                    notEmpty.await();
                }

                int value = queue.poll();
                System.out.println("消费者消费: " + value);

                // 通知生产者可以生产了
                notFull.signal();

                Thread.sleep(1000); // 模拟消费过程
            } finally {
                lock.unlock();
            }
        }
    }


    public static void main(String[] args) {
        ProducerConsumerExample example = new ProducerConsumerExample();

        Thread producerThread = new Thread(() -> {
            try {
                example.produce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumerThread = new Thread(() -> {
            try {
                example.consume();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producerThread.start();
        consumerThread.start();
    }
}
