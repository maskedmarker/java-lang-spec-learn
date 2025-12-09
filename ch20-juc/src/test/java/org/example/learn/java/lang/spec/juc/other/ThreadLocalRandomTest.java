package org.example.learn.java.lang.spec.juc.other;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * 如下是Random为了实现thread-safe,通过while+cas来保证并发正确,代价是增加了cpu开销
 * <pre><code>
 *    protected int next(int bits) {
 *         long oldseed, nextseed;
 *         AtomicLong seed = this.seed;
 *         do {
 *             oldseed = seed.get();
 *             nextseed = (oldseed * multiplier + addend) & mask;
 *         } while (!seed.compareAndSet(oldseed, nextseed)); // 保证原子性更新seed
 *         return (int)(nextseed >>> (48 - bits));
 *     }
 * </code></pre>
 *
 *
 * ThreadLocalRandom:
 * 为每个线程分配一个单独的核心数据seed,所以不存在额外的并发开销.
 */
public class ThreadLocalRandomTest {


    /**
     * 使用原始的Random类
     */
    @Test
    public void test0() {
        // 生成一个 int 类型的随机数（0 到 99）
        int randomInt = new Random().nextInt(100);
        System.out.println("随机整数: " + randomInt);

        // 生成一个指定范围的 int（10 到 20，包含 10 不包含 21）
        int rangeInt = new Random().nextInt(11) + 10;
        System.out.println("范围整数: " + rangeInt);

        // 生成 double 类型
        double randomDouble = new Random().nextDouble();
        System.out.println("随机 double: " + randomDouble);

        // 生成 boolean 类型
        boolean randomBool = new Random().nextBoolean();
        System.out.println("随机 boolean: " + randomBool);
    }

    /**
     * 使用ThreadLocalRandom
     */
    @Test
    public void test1() {
        // 生成一个 int 类型的随机数（0 到 99）
        int randomInt = ThreadLocalRandom.current().nextInt(100);
        System.out.println("随机整数: " + randomInt);

        // 生成一个指定范围的 int（10 到 20，包含 10 不包含 21）
        int rangeInt = ThreadLocalRandom.current().nextInt(10, 21);
        System.out.println("范围整数: " + rangeInt);

        // 生成 double 类型
        double randomDouble = ThreadLocalRandom.current().nextDouble();
        System.out.println("随机 double: " + randomDouble);

        // 生成 boolean 类型
        boolean randomBool = ThreadLocalRandom.current().nextBoolean();
        System.out.println("随机 boolean: " + randomBool);
    }
}
