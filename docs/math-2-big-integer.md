# BigInteger

```text
正numeral的书面字符串表示为(an-1 an-2 ... a1 a0)b
numeral的值value=(an-1)*b^(n-1) + (an-2)*b^(n-2) + ... + (a1)*b^1 + (a0)*b^0

numeral的值value指的是十进制表示下的值(没有特殊表示,指的都是十进制的值)
b^(n-1)对应的名词是位权
(an-1)该symbol对应十进制的数值
```

````text
BigInteger将数值的正负号和绝对值分开对待的.

主要属性:
final int signum;
final int[] mag;

signum存放符号,signum为0即代表0；signum为1即代表正数、signum为-1即代表负数
mag数组仅仅用来存放绝对值

BigInteger通过int[]以2^32进制的形式保存数值.
2~36(10数字+26字母)进制下,一个数字或字母就可以表示该进制下的一个symbol,但是2^32进制下的symbol无法用某个数字或字母表示,所以只强调symbol的数值
每个int值就是一个2^32进制的symbol的数值.

比如:
(10)2的数值(十进制)是2
(A3)16的数值是163
上面(10)2中的1的symbol的数值刚好对应1,0的symbol的数值刚好对应1,所以(10)2的数值=1*(2^1)+0*(2^0)=2
上面(A3)16中的A的symbol的数值刚好对应10,3的symbol的数值刚好对应3,所以(A3)16的数值=10*(16^1)+3*(16^0)=163

在2^32进制下,每个symbol的数值用int[]的每个元素来容纳,那么
[287, 1912276171]的数值就是287*(2^32^1)+1912276171*(2^32^0)=1234567890123
````



BigInteger的mag数组仅仅用来存放绝对值的二进制位，其符号被signum存放，signum为0即代表0；signum为1即代表正数、signum为-1即代表负数
为什么mag数组中符号位要单独拎出来？就是说为什么大数不用补码表示，每个元素的符号位其实不参与表示大数。 
- 不用补码存储可以按照我们传统的计算思路完成运算 
- 如果采用补码，乘除等操作将会变得很复杂，并且，获取相反数、绝对值等算法的复杂度也会由常数变为线性

```text
主要属性:
final int signum;
final int[] mag;

final表示在BigInteger构造函数中就已经确定了signum和mag的值.

signum
The signum of this BigInteger: -1 for negative, 0 for zero, or 1 for positive. 
Note that the BigInteger zero must have a signum of 0. 
This is necessary to ensures that there is exactly one representation(只有一种表示形式) for each BigInteger value.

mag
The magnitude of this BigInteger, in big-endian order: the zeroth element of this array is the most-significant int of the magnitude. 
The magnitude must be "minimal" in that the most-significant int (mag[0]) must be non-zero. 
This is necessary to ensure that there is exactly one representation(只有一种表示形式) for each BigInteger value. 
Note that this implies that the BigInteger zero has a zero-length mag array.

in big-endian order
数组mag的index类比内存地址,index较小的元素存储更重要的数据.即the zeroth element of this array is the most-significant int of the magnitude
```

```text
value = signum × (Σ mag[i] × 2^(32 * (len-1 - i)))

i的取值范围[0, mag.len-1]
```


```text
java.math.BigInteger.BigInteger(java.lang.String)

BigInteger支持简单的正/负号
BigInteger支持leading-zeros
BigInteger不支持科学计数法
```



```text
字符串val中每个字符都是radix进制的一个digit.
BigInteger(String val, int radix)将radix进制的数据转换为2^32进制的数据

public BigInteger(String val, int radix) {
    // cursor表示正要解析的字符的index(0-based)
    int cursor = 0, numDigits;
    final int len = val.length();

    //只支持2~36进制
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
        throw new NumberFormatException("Radix out of range");
    if (len == 0)
        throw new NumberFormatException("Zero length BigInteger");

    // Check for at most one leading sign (只支持简单的正/负号)
    int sign = 1;
    int index1 = val.lastIndexOf('-');
    int index2 = val.lastIndexOf('+');
    if (index1 >= 0) {
        if (index1 != 0 || index2 >= 0) {
            throw new NumberFormatException("Illegal embedded sign character");
        }
        sign = -1;
        cursor = 1;
    } else if (index2 >= 0) {
        if (index2 != 0) {
            throw new NumberFormatException("Illegal embedded sign character");
        }
        cursor = 1;
    }
    if (cursor == len)
        throw new NumberFormatException("Zero length BigInteger");

    // Skip leading zeros and compute number of digits in magnitude(支持leading-zeros,不支持科学计数法)
    while (cursor < len && Character.digit(val.charAt(cursor), radix) == 0) {
        cursor++;
    }

    // 支持digits都是零,即数值为0
    if (cursor == len) {
        signum = 0;
        mag = ZERO.mag;
        return;
    }

    // 因为cursor是0-based,所以len-cursor即为有效的digits
    numDigits = len - cursor;
    signum = sign;

    // bitsPerDigit[radix] gives an approximate number of bits per digit for that radix
    long numBits = ((numDigits * bitsPerDigit[radix]) >>> 10) + 1;
    
    // 因为numBits是long类型的,为了防止后面的(int) (numBits + 31)在强转是发生截断,这里要提前保证(numBits + 31)不能大于等于int最大值
    if (numBits + 31 >= (1L << 32)) {
        reportOverflow();
    }
    // 按32个bit为一组分组, 即按word(一个word为2^5bit)进行分组
    int numWords = (int) (numBits + 31) >>> 5;
    int[] magnitude = new int[numWords];

    // Why groups?
    // Because a BigInteger’s internal representation is an array of 32-bit words. They process the input string in chunks (each “group”) that fit in an int. BigInteger的mag字段是int[]类型,所以按int长度32个bit分组
    
    // Process first (potentially short) digit group
    // numDigits % digitsPerInt[radix] 余数即为剩余的最左侧部分
    int firstGroupLen = numDigits % digitsPerInt[radix];
    if (firstGroupLen == 0)
        firstGroupLen = digitsPerInt[radix];
    String group = val.substring(cursor, cursor += firstGroupLen);
    magnitude[numWords - 1] = Integer.parseInt(group, radix);
    if (magnitude[numWords - 1] < 0)
        throw new NumberFormatException("Illegal digit");

    // Process remaining digit groups
    int superRadix = intRadix[radix];
    int groupVal = 0;
    while (cursor < len) {
        group = val.substring(cursor, cursor += digitsPerInt[radix]);
        groupVal = Integer.parseInt(group, radix);
        if (groupVal < 0)
            throw new NumberFormatException("Illegal digit");
        destructiveMulAdd(magnitude, superRadix, groupVal);
    }
    // Required for cases where the array was overallocated.
    mag = trustedStripLeadingZeroInts(magnitude);
    if (mag.length >= MAX_MAG_LENGTH) {
        checkRange();
    }
}
```


```text
bitsPerDigit

bitsPerDigit是 BigInteger 里一个非常关键的静态表，用于估算不同进制的数字所占的二进制位数（bit 数）
注意：在不同 JDK 版本中，这段代码可能稍有不同，但原理相同。有的版本是预先计算好的常量表，有的版本在静态块中动态计算。


/**
 * bitsPerDigit[radix] contains the average number of bits per digit in the given radix. Values are multiplied by 1024 and rounded up.
 */
private static final double LOG_TWO = Math.log(2.0);
private static final int[] bitsPerDigit = new int[37];
static {
    for (int i = Character.MIN_RADIX; i <= Character.MAX_RADIX; i++) {
        bitsPerDigit[i] = (int)Math.ceil(1024 * (Math.log(i) / LOG_TWO));
    }
}

它表示什么含义？
bitsPerDigit[radix] ≈ 在该进制下，一个数字平均占用的二进制位数 × 1024。
举个例子：
radix（进制）	每位数字所代表的二进制位数	bitsPerDigit 值
2	               1 bit	              1024
8	               3 bits	              3072
10	               ~3.32 bits	          3402
16	               4 bits	              4096
36	               ~5.17 bits	          5290


为什么乘以 1024？
因为 bitsPerDigit 被存储为整数，而真实值可能是小数（比如 3.321928...）。
为了保留精度，源码中将其 乘以 1024 (即 2¹⁰)，然后存入 int,当使用时，就通过位运算右移 10 位来还原：
numBits = ((numDigits * bitsPerDigit[radix]) >>> 10) + 1;
等价于：
numBits ≈ numDigits * (bitsPerDigit[radix] / 1024)
也就是：
估算 numDigits 个数字大约需要多少 bit。

使用场景: numBits = ((numDigits * bitsPerDigit[radix]) >>> 10) + 1; 其中加1为了防止(numDigits * bitsPerDigit[radix])/1024F的浮点数有小数部分被舍弃

举例说明
以十进制字符串 "9999999999"（10 位）为例：
bitsPerDigit[10] ≈ 3402
numDigits = 10
numBits = ((10 * 3402) >>> 10) + 1
         = (34020 >>> 10) + 1
         = 33 + 1 = 34 bits
实际 9999999999 的二进制为：
1001010100000010111110011111111111
实际是 34 bits
如果没有 +1，结果会是 33 bits，略小，可能导致分配空间不足。         
```


```text
digitsPerInt

在 BigInteger 源码中，你可以看到这样一段静态表定义
private static int digitsPerInt[] = {0, 0, 30, 19, 15, 13, 11, 11, 10, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5};
这个数组的索引代表进制（radix）,数组的值就是该进制下，一个 int 能安全容纳的最大位数.

假设进制为r,可以容纳n个digit(n为正整数),那么n个digit组成最大值的十进制为r^n-1
n <= logr(Integer.MAX_VALUE+1)
为了保险,干脆取n=logr(Integer.MAX_VALUE),也就是意味着还可能留有空余空间.

```

```text
intRadix

digitsPerInt[radix] 决定了“每组(即每个int)用来容纳多少个digit”
而 intRadix[radix] 决定了“每组之间的进位倍数”

intRadix[radix] = radix ^ digitsPerInt[radix]
```


# TODO

```text
临时记录,后续整理

n进制下,每个digit不一定是单个字符的,比如1024进制下,1021可以看作是1024进制下的一个digit

2种进制转换时,十进制是他们的转换桥梁

(1100_0000)2 
    = 2^7+2^6
    = (2^3+2^2)*(2^4)
    = 12*(2^4)
    = Integer.parseInt("1100", 2) * (2^4)

上面的计算中, 除了用2进制字符串表达外,都是十进制来表示数值.


延申:
将二进制字符串1100_0000,按5个为一组
(1100_0000)2 
    = (110_00000)2 
    = 2^7+2^6
    = (2^2+2^1)*(2^5)
    = 5*(2^5)
    = Integer.parseInt("110", 2) * (2^5)

二进制的1100_0000,用32进制来表示5*(32^1)+0*(32^0)


将二进制字符串1100_0000_0000_0000_0000_0000_0000_0000_0000_0000,按10个为一组
(1100_0000_0000_0000_0000_0000_0000_0000_0000_0000)2
    = (1100000000_0000000000_0000000000_0000000000)2
    = (2^9+2^8)*(2^10)^3 + 0*(2^10)^2 + 0*(2^10)^1 + 0*(2^10)^0
    = (2^9+2^8)*(2^30)
    = 768*(2^30)
    = Integer.parseInt("1100000000", 2) * (2^30)
    
二进制的1100_0000_0000_0000_0000_0000_0000_0000_0000_0000,用1024进制来表示为(768)0
其中(768)整体可以看作是1024进制下的一个digit

上面所有的计算都是通过十进制来作为进制转换的桥梁.
```


```text
⚡ destructiveMulAdd

类似我们做字符串转正整数时的进位操作

destructiveMulAdd(magnitude, superRadix, groupVal);
=> magnitude = magnitude * superRadix + groupVal


private static void destructiveMulAdd(int[] x, int y, int z) {
    long ylong = y & 0xffffffffL;
    long zlong = z & 0xffffffffL;
    long carry = 0;

    for (int i = x.length - 1; i >= 0; i--) {
        long product = (x[i] & 0xffffffffL) * ylong + carry;
        x[i] = (int) product;
        carry = product >>> 32;
    }

    // add z to the least significant word
    long sum = (x[x.length - 1] & 0xffffffffL) + zlong;
    x[x.length - 1] = (int) sum;
    carry = sum >>> 32;

    // propagate carry if necessary
    for (int i = x.length - 2; carry != 0 && i >= 0; i--) {
        long v = (x[i] & 0xffffffffL) + carry;
        x[i] = (int) v;
        carry = v >>> 32;
    }
}

It modifies the magnitude array x in place, performing this arithmetic:
x = x * y + z


When parsing the string "123456789" in base 10
we might read it as groups:
["123", "456", "789"]
and we process them sequentially:
value = (((0 * 10³ + 123) * 10³ + 456) * 10³ + 789)
```


```text
简单来说，BigInteger的底层是利用一个int的数组（称为mag）存储大整数，也就是说，当要存放的整数大于32位时，就会被分割成32位为一组的形式，每一组就作为底层数组的一个元素。
并且，BigInteger的底层数组mag时大端存放的，也就是说mag[0]、mag[1]...mag[mag.length - 1]分别代表整数的最高32位、次高32位...最低32位。
并且, BigInteger的mag数组仅仅用来存放绝对值的二进制位，其符号被signum存放，signum为0即代表0；signum为1即代表正数、signum为-1即代表负数。

为什么不直接按照补码存放呢? 我认为，是为了使操作的算法更为高效和方便。
如果采用补码，* /等操作将会变得很复杂，并且，获取相反数、绝对值等算法的复杂度也会由常数变为线性。
由于底层数组是final的，仅仅想改改符号位也是不可能的，必须要深拷贝一份, 如果有了signum存放符号, 求个相反数只需要求反个signum, 拷贝mag只需浅拷贝。
有了signum使判断是否为0十分方便。
```