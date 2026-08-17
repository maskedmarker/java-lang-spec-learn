package org.example.learn.java.lang.spec.time;



import org.junit.Test;

import java.time.*;

/**
 * LocalDate 的定位非常明确且专一
 * 它是一个“不含时区信息”的“纯日期”对象，只负责表示年、月、日（年月日）
 * 它的核心设计哲学是 “简单、不可变、线程安全”，专门用于处理那些不需要时分秒，也不需要时区的日常日期场景。
 *
 * 它最大的特点（与旧 Date 类的区别）
 * 如果你用过旧的 java.util.Date，你会发现它明明叫“日期”，却既包含日期又包含时间，还带着时区，非常混乱。
 * java.util.Date：实际是时间戳（毫秒数），打印出来还要转时区，容易造成误解。
 * LocalDate：就是日历上看到的那一行字。比如“2026-08-13”在全世界任何地方的电脑上打印出来都是“2026-08-13”，不存在时区转换问题。
 *
 *
 * 如果需要一个精确到秒的具体时刻，应该用：
 * LocalDateTime（日期+时间，但无时区）
 * Instant（时间戳，面向机器, 带时区概念的：永远代表UTC+0, 没有ZoneId字段,但api约定）
 * ZonedDateTime（日期+时间+时区）
 *
 * 理解了 LocalDate 的定位后，之前的结论就更清晰了：
 * Period 天生就是为了配合 LocalDate 设计的（Period.between(LocalDate, LocalDate)）。
 */
public class TimePointTest {

    // region 不含时区概念
    // 如何创建和简单使用
    @Test
    public void test01() {
        // 1. 获取今天（系统默认时区的今天）
        LocalDate today = LocalDate.now();

        // 2. 指定具体日期
        LocalDate date = LocalDate.of(2026, 8, 13);

        // 3. 解析字符串
        LocalDate parsed = LocalDate.parse("2026-08-13");

        // 4. 日期运算（返回新对象，原对象不变）
        LocalDate nextWeek = date.plusWeeks(1);        // 2026-08-20
        LocalDate previousMonth = date.minusMonths(1); // 2026-07-13

        // 5. 获取局部信息
        int year = date.getYear();        // 2026
        Month month = date.getMonth();    // AUGUST
        int dayOfWeek = date.getDayOfWeek().getValue(); // 4 (周四)
        boolean isLeap = date.isLeapYear(); // false (2026不是闰年)
    }

    @Test
    public void test02() {
        // 1. LocalDateTime：只有日期时间，没有时区
        LocalDateTime ldt = LocalDateTime.of(2026, 8, 13, 10, 0, 0);
        System.out.println(ldt);
        // 输出：2026-08-13T10:00（只是一个“字符串”，不是精确时刻）
    }


    // region 含时区概念

    @Test
    public void test12() {
        // 2. ZonedDateTime：日期时间 + 时区
        ZonedDateTime zdtTokyo = ZonedDateTime.of(2026, 8, 13, 10, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime zdtLondon = zdtTokyo.withZoneSameInstant(ZoneId.of("Europe/London"));

        System.out.println(zdtTokyo);   // 2026-08-13T10:00+09:00[Asia/Tokyo]
        System.out.println(zdtLondon);  // 2026-08-13T02:00+01:00[Europe/London] （夏令时，差8小时）
    }

    // region 它们之间的转换关系（必须有时区作为桥梁）
    @Test
    public void test21() {
        LocalDateTime ldt = LocalDateTime.now();
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    public void test22() {
        ZonedDateTime zdt = ZonedDateTime.now();
        LocalDateTime ldt = zdt.toLocalDateTime(); // 丢掉时区，只保留年月日时分秒
    }

    @Test
    public void test23() {
        ZonedDateTime zdt = ZonedDateTime.now();
        Instant instant = zdt.toInstant(); // 转换为 UTC 时间戳
    }
}
