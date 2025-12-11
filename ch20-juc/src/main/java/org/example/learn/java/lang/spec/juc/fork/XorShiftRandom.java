package org.example.learn.java.lang.spec.juc.fork;

/**
 *  伪随机数生成器(PRNG)的核心运算是经典的xorshift类算法或其变种，用于通过位操作高效地生成看似随机的序列
 *
 *  在32位系统中,
 *      6, 21, 7 的和为 34，大于32，确保了好的扩散
 *
 *  优点和缺点
 *      优点: 极快/相同的输入产生相同的输出/好的参数选择可以获得2^32-1的周期
 *      缺点：可通过线性分析预测/位移量(6, 21, 7)需要精心选择
 *
 *  实际应用场景
 *      简单的整数哈希函数
 *      轻量级RNG
 */
public class XorShiftRandom {

    private int seed;

    public XorShiftRandom(int seed) {
        this.seed = seed;
    }

    public int nextInt() {
        // 这种"左移-右移-左移"的模式能够有效地扩散位信息，让原始值的微小变化传播到整个字长
        seed ^= seed << 6;    // 混合低位信息
        seed ^= seed >>> 21;  // 将高位信息带到低位
        seed ^= seed << 7;    // 再次混合

        return seed;
    }
}
