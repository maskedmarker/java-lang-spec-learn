# BigInteger

BigInteger可以表示工程上的"无穷大/小"的整数;BigDecimal可以表示工程上的"无穷大/小"的浮点数

```text
注意:

没有特殊表示,所有的值指的都是十进制的值
```

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
final int signum; // signum存放符号,signum为0即代表0；signum为1即代表正数、signum为-1即代表负数
final int[] mag;  // mag数组仅仅用来存放绝对值


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

BigInteger表示的整数值value为:
value = signum × (Σ mag[i] × 2^(32 * (len-1 - i)))
i的取值范围[0, mag.len-1]
````

```text
加法的运算过程模拟

2个数分别为 a4a3a2a1  b3b2b1
低位对齐,高位缺少时按0处理

 a4a3a2a1
 0 b3b2b1
-----------------
c4d4d3d2d1

(a1+b1)/10=c1 (a1+b1)%10=d1
(a2+b2+c1)/10=c2 (a2+b2+c1)%10=d2
(a3+b3+c2)/10=c3 (a3+b3+c2)%10=d3
(a3+0+c3)/10=c4 (a3+0+c3)%10=d4
```

```text
整数进位后加值

在整数a4a3a2a1尾部追加a5
a4a3a2a1*10=(a4*10^3+a3*10^2+a2*10^1+a1)*10=a4*10*10^3+a3*10*10^2+a2*10*10^1+a1*10
a4a3a2a1*10+a5=a4*10*10^3+a3*10*10^2+a2*10*10^1+a1*10+a5=a4a3a2a1a5

因为carry=(an*10)/10=an, (an*10)%10=0, 所以每一位都是直接进一位

如果整数a4a3a2a1乘以x,则
a4a3a2a1*x
 =(a4*10^3+a3*10^2+a2*10^1+a1)*x
 =a4*x*10^3+a3*x*10^2+a2*x*10^1+a1*x
 ={a4*x}*10^3+{a3*x}*10^2+{a2*x}*10^1+{a1}*x
 ={((a4*x)/10)*10+(a4*x)%10}*10^3+{((a3*x)/10)*10+(a3*x)%10}*10^2+{((a2*x)/10)*10+(a2*x)%10}*10^1+{((a1*x)/10)*10+(a1*x)%10}
 ={((a4*x)/10*10)}*10^3+{(a4*x)%10}*10^3 +{((a3*x)/10)*10}*10^2+{(a3*x)%10}*10^2 +{((a2*x)/10)*10}*10^1+{(a2*x)%10}*10^1 +{((a1*x)/10)*10}+{(a1*x)%10}
 ={((a4*x)/10)}*10^4 + {(a4*x)%10}*10^3+{((a3*x)/10)}*10^3 + {(a3*x)%10}*10^2+{((a2*x)/10)}*10^2 +{(a2*x)%10}*10^1+{((a1*x)/10)}*10^1 +{(a1*x)%10}
 ={((a4*x)/10)}*10^4 + {(a4*x)%10+((a3*x)/10)}*10^3 + {(a3*x)%10+((a2*x)/10)}*10^2 +{(a2*x)%10+((a1*x)/10)}*10^1 +{(a1*x)%10}

注意,x不能大于进制乘数

如果将整数用数组(向量)来表示(十进制),
a4a3a2a1 -> [a4, a3, a2, a1]
a4a3a2a1*10 -> [a4, a3, a2, a1, 0]
a4a3a2a1*10+a5 -> [a4, a3, a2, a1, a5]
a4a3a2a1*x -> [((a4*x)/10), (a4*x)%10+((a3*x)/10), (a3*x)%10+((a2*x)/10), (a2*x)%10+((a1*x)/10), (a1*x)%10]

(a4*x)%10+((a3*x)/10)中((a3*x)/10)是最近的低位的carry
```

```text
destructiveMulAdd()是构造BigInteger的核心的算法部分.
bitsPerDigit/digitsPerInt都是用来预估内存分配空间的,先不用管(假定内存空间足够).

destructiveMulAdd()的算法过程用如下类比:
读取10进制数的字符串"12345",并求出其大小
读取字符1      临时大小为1*10^0 =1 
读取字符12     临时大小为(1*10^0)*10^1 + 2*10^0 =12
读取字符123    临时大小为((1*10^0)*10^1 + 2*10^0)*10^1 + 3*10^0 =123
读取字符1234   临时大小为(((1*10^0)*10^1 + 2*10^0)*10^1 + 3*10^0)*10^1 + 4*10^0  =1234
读取字符12345  临时大小为1((((1*10^0)*10^1 + 2*10^0)*10^1 + 3*10^0)*10^1 + 4*10^0)*10^1 + 5*10^0 =12345
因为没有新的字符,所以读取完毕,最终该字符串的大小值为12345

在读取字符串的过程中,因为无法提前知道字符串有多长(也就无法确定知道第一个字符在哪个位权),所以每当新读取一个字符,就将前面已经读取的数值再乘以一个进位值(因为之前的错判导致少了一个进位).
上面仅仅是解决了解析字符串的值,并没有处理在解析过程中,这些临时值怎么存储.

怎么存储这个问题跟字符编码类似,我们先来回顾一下字符编码
code-point用来表示字符在字符编码中该字符的值(这是一个逻辑上的数值)
code-point是一个逻辑上的数值,具体怎么展示(或者书写)还没确定,encoding-form用来解决展示的问题.将code-point值用二进制表示,获得一个常常的1-0序列,按一定长度切分为多个段,每段就是一个code-unit


怎么存储这个问题又包含2个子问题:
1.用什么数据结构存储
2.那么基于上面的数据结构,保存数值是按照怎么样的方案来将数值拆分并保存的(其实就是按照不同的数据结构,选定相应的进制),读取时就是按相应的方案组装.


用int[]类型的mag来容纳临时值,同时采用十进制.
mag为[0],读取字符到1, int[1]能存储不大于9的数值,mag为[1]
mag为[1],读取字符到12,int[1]不能存储大于9的数值,int[2]能存储不大于99(9*10+9)的数值,扩容为int[2],12-10=2,mag为[1, 2]
mag为[1, 2],读取字符到123,int[2]不能存储大于99的数值,int[3]能存储不大于999(9*100+9*10+9)的数值,扩容为int[3],123-100=23|23-20=3,mag为[1, 2, 3]
mag为[1, 2, 3],读取字符到1234,int[3]不能存储大于999的数值,int[4]能存储不大于9999(9*1000+9*100+9*10+9)的数值,扩容为int[4],1234-1000=234|234-200=34|34-30=4,mag为[1, 2, 3, 4]
mag为[1, 2, 3, 4],读取字符到12345,int[4]不能存储大于9999的数值,int[5]能存储不大于99999(9*10000+9*1000+9*100+9*10+9)的数值,扩容为int[4],12345-10000=2345|2345-2000=345|345-300=45|45-40=5,mag为[1, 2, 3, 4, 5]
----------------------
上面是笨方法,下面采用更科学直观的方法
用int[]类型的mag来容纳临时值,同时采用十进制,同时假定
mag为[0],读取字符到1, 1%10=1,carry=1/10=0,carry为0无需进位扩容,mag为[1]
mag为[1],读取字符到2,12%10=2,carry=12/10=1,carry为1,扩容为int[2],mag为[1, 2]
mag为[1, 2],读取字符到123,int[2]不能存储大于99的数值,int[3]能存储不大于999(9*100+9*10+9)的数值,扩容为int[3],123-100=23|23-20=3,mag为[1, 2, 3]
mag为[1, 2, 3],读取字符到1234,int[3]不能存储大于999的数值,int[4]能存储不大于9999(9*1000+9*100+9*10+9)的数值,扩容为int[4],1234-1000=234|234-200=34|34-30=4,mag为[1, 2, 3, 4]
mag为[1, 2, 3, 4],读取字符到12345,int[4]不能存储大于9999的数值,int[5]能存储不大于99999(9*10000+9*1000+9*100+9*10+9)的数值,扩容为int[4],12345-10000=2345|2345-2000=345|345-300=45|45-40=5,mag为[1, 2, 3, 4, 5]

123-100=23|23-20=3的计算逻辑是这样的:
因为要用int[3]容纳,
先满足最高位的百位, 1*100<123<2*100, 百位选取1, 剩余的123-100=23由int[2]处理;
再满足十位, 2*10<23<3*10,十位选取2, 剩余的23-20=3由int[1]处理;
最后满足个位.
-------------------------------------------------

上面是存储过程,如果要读取数值,则为mag[4]*10000+mag[3]*1000+mag[2]*100+mag[1]*10+mag[0]
当使用int[]类型时,mag[4]/mag[3]/mag[2]/mag[1]/mag[0],对应的数字1/2/3/4/5都没有占满int的32bit(最多占了3bit),有点浪费内存空间,可以扩大进制/或者缩小数组的类型(比如使用short/byte)


用byte[]类型的mag来容纳临时值,同时采用256(2^8)进制.
mag为[0],读取字符到1, byte[1]能存储不大于255的数值,mag为 [1]
mag为[1],读取字符到12,byte[1]能存储不大于255的数值,mag为[12]
mag为[1, 2],读取字符到123,byte[1]能存储不大于255的数值,mag为[123]
mag为[1, 2, 3],读取字符到1234,byte[1]不能存储大于255的数值,byte[2]能存储不大于65535(255*256+255)的数值,扩容为byte[2],1234-4*256=210,mag为[4, 256]
mag为[1, 2, 3, 4],读取字符到12345,byte[2]能存储大于65535的数值,12345-48*256=67|,mag为[48, 67]

mag占用的内存空间为mag[1]=(48)10=(00110000)2,mag[0]=(67)10=(01000011)2,相比于int[],内存使用率提高了太多,因为256(2^8)进制能尽可能使用byte的8个bit位.如果使用128(2^7)进制,则损失大概1/8的内存空间.
所以如果使用int[]数据结构,则采用2^32进制可以最大效率使用内存空间.


至此,我们可以得出结论:
如果使用int[]来存储数据,那么为了提高内存使用率,需要采用2^32进制.
-------------------------------------------------


destructiveMulAdd()的算法过程用如下类比:
mag用int[]类型的数据结构,同时采用2^32进制(假定数组的长度自动扩容).
读取10进制数的字符串"1234567890123",并求出其大小
读到字符1    临时大小为1,1%2^32=1,carry=1/2^32=0,mag为[1]
读到字符2    临时大小为mag[0]*10+2=12,12%2^32=12,carry=12/2^32=0,mag为[12]
读到字符3    临时大小为mag[0]*10+3=123,123%2^32=123,carry=123/2^32=0,mag为[123]
读到字符4    临时大小为mag[0]*10+4=1234,1234%2^32=1234,carry=1234/2^32=0,mag为[1234]
读到字符5    临时大小1mag[0]*10+5=12345,12345%2^32=12345,carry=12345/2^32=0,mag为[12345]
....




-------------------------------------------------

在8进制下,用int[]类型的mag容纳临时大小, 2^8=256
mag为[0],读取字符到1, mag为 [1]

```

```text
在十进制下,用int[]类型的mag容纳临时值,
mag为[0],读取字符到1, mag为 [1]
mag为[1],读取字符到12,mag[0]升位,mag为[1, 0], 从个十位中间切开, mag为 [1, 2]
mag为[1, 2],读取字符到123,mag[1]升位,mag为[1, 2, 0], 从十百位中间切开,mag为 [1, 2, 3]
mag为[1, 2, 3],读取字符到1234,mag[2]升位,mag为[1, 2, 3, 0], 从百千位中间切开, mag为 [1, 2, 3, 4]
mag为[1, 2, 3, 4],读取字符到12345,mag[3]升位,mag为[1, 2, 3, 4, 0], 从千万位中间切开, mag为 [1, 2, 3, 4, 5]

注意: 整个过程中,不涉及到carry.
```


```text
为什么mag数组中符号位要单独拎出来？就是说为什么大数不用补码表示，每个元素的符号位其实不参与表示大数。 
- 不用补码存储可以按照我们传统的计算思路完成运算 
- 如果采用补码，乘除等操作将会变得很复杂，并且，获取相反数、绝对值等算法的复杂度也会由常数变为线性
```

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
字符串val中每个字符都是radix进制的一个symbol.
BigInteger(String val, int radix)将radix进制的数据转换为2^32进制的数据

java.math.BigInteger.BigInteger(java.lang.String)
BigInteger支持简单的正/负号
BigInteger支持leading-zeros
BigInteger不支持科学计数法

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