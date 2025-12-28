package org.example.learn.java.lang.spec.math.bit;

/**
 * 计算机中,加减法会因为进位导致无法保证仅仅操作byte的某个bit,而位运算符可以操作时,不涉及进位,所以可以精确操控byte的某个bit
 *
 * 对bit的操控可以抽象成如下4个操作
 * set(word, n)
 * clear(word, n)
 * toggle(word, n)
 * read(word, n)
 *
 * 如上word指的就是cpu处理的最小单元,n指的是操控word中索引值为n的那个bit(索引从右起始,0-based)
 *
 *
 * &0会保证bit值必为0
 * |1会保证bit值必为1
 * |0不会变更原来的bit值; &1也不会变更原来的bit值
 */
public class BitwiseCompute {

    public static int set(int word, int n) {
        // java不支持大于32的bit-shift
        if (n > 32) {
            throw new RuntimeException("n > 32");
        }

        // 通过 x | (000000010000)来精确为某个bit设置1    (|1会保证bit值必为1, |0不会变更原来的bit值)
        int mask = (1 << n);
        return word | mask;
    }

    public static int clear(int word, int n) {
        if (n > 32) {
            throw new RuntimeException("n > 32");
        }

        // 通过 x & (111111101111)来精确清除某个bit,即设置为0  (&0会保证bit值必为0, &1也不会变更原来的bit值)
        int mask = ~(1 << n);
        return word & mask;
    }

    /**
     *  异或位运算^中,假设a/b都是某个bit值
     *  a ^ b
     *     当b为1时, (a^b)值与a的值相反
     *     当b为0时, (a^b)值与a的值相同
     */
    public static int toggle(int word, int n) {
        if (n > 32) {
            throw new RuntimeException("n > 32");
        }

        // 通过 x | (000000010000)来精确反转某个bit
        int mask = (1 << n);
        return word ^ mask;
    }

    public static int read(int word, int n) {
        if (n > 32) {
            throw new RuntimeException("n > 32");
        }

        // 通过 x & (0000001000)来精确保留某个bit值,同时清空其他bit值, 然后右移位来清空进制
        int mask = (1 << n);
        return (word & mask) >> n;
    }

    public static int read2(int word, int n) {
        if (n > 32) {
            throw new RuntimeException("n > 32");
        }

        // 先将所需的bit右移到最低位,在通过00001来清空其他位的bit值
        return (word >> n) & 1;
    }
}
