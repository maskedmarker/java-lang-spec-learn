# 关于字符编码

工具网站: https://www.fileformat.info/

## 编码基础概念
```text
A character is a minimal unit of text that has semantic value.
A character set is a collection of elements used to represent text.
A coded character set is a character set mapped to a set of unique numbers.
    For historical reasons, this is also often referred to as a code page
A character repertoire is the set of characters that can be represented by a particular coded character set.
A code point is a value or position of a character in a coded character set.
A code space is the range of numerical values spanned by a coded character set.

A code unit is the minimum bit combination that can represent a character in a character encoding (in computer science terms, it is the word size of the character encoding).
For example, common code units include 7-bit, 8-bit, 16-bit, and 32-bit. 
In some encodings, some characters are encoded using multiple code units; such an encoding is referred to as a variable-width encoding.
```



## UTF-16

```text
UTF-16 (16-bit Unicode Transformation Format) is a character encoding that supports all 1,112,064 valid code points of Unicode.
The encoding is variable-length as code points are encoded with one or two 16-bit code units.


Each Unicode code point is encoded either as one or two 16-bit code units. 
Code points less than 216 ("in the BMP") are encoded with a single 16-bit code unit equal to the numerical value of the code point, as in the older UCS-2. 
Code points greater than or equal to 216 ("above the BMP") are encoded using two 16-bit code units. 
These two 16-bit code units are chosen from the UTF-16 surrogate range 0xD800–0xDFFF which had not previously been assigned to characters. 
Values in this range are not used as characters, and UTF-16 provides no legal way to code them as individual code points. 
A UTF-16 stream, therefore, consists of single 16-bit codes outside the surrogate range, and pairs of 16-bit values that are within the surrogate range.


U+0000 to U+D7FF  and  U+E000 to U+FFFF
Both UTF-16 and UCS-2 encode code points in this range as single 16-bit code units that are numerically equal to the corresponding code points. 

U+D800 to U+DFFF have a special purpose, see below.

Code points from U+010000 to U+10FFFF
Code points from the other planes are encoded as two 16-bit code units called a surrogate pair. 
The first code unit is a high surrogate and the second is a low surrogate (These are also known as "leading" and "trailing" surrogates, respectively, analogous to the leading and trailing bytes of UTF-8).


Byte-order encoding schemes
UTF-16 and UCS-2 produce a sequence of 16-bit code units. Since most communication and storage protocols are defined for bytes, and each unit thus takes two 8-bit bytes, the order of the bytes may depend on the endianness (byte order) of the computer architecture.
To assist in recognizing the byte order of code units, UTF-16 allows a byte order mark (BOM), a code point with the value U+FEFF, to precede the first actual coded value.[c] (U+FEFF is the invisible zero-width non-breaking space/ZWNBSP character).



To encode U+10437 (𐐷) to UTF-16:

Subtract 0x10000 from the code point, leaving 0x0437.
For the high surrogate, shift right by 10 (divide by 0x400), then add 0xD800, resulting in 0x0001 + 0xD800 = 0xD801.
For the low surrogate, take the low 10 bits (remainder of dividing by 0x400), then add 0xDC00, resulting in 0x0037 + 0xDC00 = 0xDC37.
To decode U+10437 (𐐷) from UTF-16:

Take the high surrogate (0xD801) and subtract 0xD800, then multiply by 0x400, resulting in 0x0001 × 0x400 = 0x0400.
Take the low surrogate (0xDC37) and subtract 0xDC00, resulting in 0x37.
Add these two results together (0x0437), and finally add 0x10000 to get the final code point, 0x10437.

```