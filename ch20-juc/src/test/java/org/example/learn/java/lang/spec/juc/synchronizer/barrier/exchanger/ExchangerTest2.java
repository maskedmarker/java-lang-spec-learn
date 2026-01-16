package org.example.learn.java.lang.spec.juc.synchronizer.barrier.exchanger;

import org.junit.Test;

import java.util.concurrent.Exchanger;


/**
 * Exchanger复杂使用场景
 */
public class ExchangerTest2 {


    /**
     * 多个Exchanger来协调工作
     */
    @Test
    public void test01() throws InterruptedException {
        final Exchanger<String> dataExchanger = new Exchanger<>();
        final Exchanger<Boolean> resultExchanger = new Exchanger<>();

        // 数据校验线程
        Thread validator = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    String data = dataExchanger.exchange(null);
                    System.out.println("校验器: 收到数据 '" + data + "'");

                    // 模拟校验逻辑
                    boolean isValid = data != null && data.length() > 0;
                    Thread.sleep(500); // 模拟校验时间

                    // 返回校验结果
                    resultExchanger.exchange(isValid);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 数据处理线程
        Thread processor = new Thread(() -> {
            try {
                String[] dataArray = {"Hello", "", "World"};

                for (String data : dataArray) {
                    System.out.println("处理器: 发送数据 '" + data + "' 进行校验");

                    // 发送数据给校验器
                    dataExchanger.exchange(data);

                    // 获取校验结果
                    boolean isValid = resultExchanger.exchange(null);

                    if (isValid) {
                        System.out.println("处理器: 数据 '" + data + "' 校验通过，进行处理");
                        // 处理数据...
                    } else {
                        System.out.println("处理器: 数据 '" + data + "' 校验失败，丢弃");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        processor.start();
        validator.start();

        // 等待工作线程自然结束,防止junit在测试方法结束后主动杀死工作线程
        processor.join();
        validator.join();
    }
}
