package org.example.learn.java.lang.spec.time;


import org.junit.Test;

import java.time.*;

public class TimeSpanTest {

    // region day-based
    // 注意：Period 只算"年月日"的差额，不会把6年换算成总天数（因为闰年影响） 受日历规则影响
    @Test
    public void test01() {
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2026, 8, 13);
        Period period = Period.between(start, end);
        System.out.println(period.getYears());   // 6
        System.out.println(period.getMonths());  // 7
        System.out.println(period.getDays());    // 12
    }

    @Test
    public void test02() {
        LocalDate start = LocalDate.of(2020, 2, 27);
        LocalDate end = LocalDate.of(2021, 3, 1);
        Period period = Period.between(start, end);
        System.out.println(period.getYears());   // 1
        System.out.println(period.getMonths());  // 0
        System.out.println(period.getDays());    // 2
    }

    @Test
    public void test03() {
        LocalDate start = LocalDate.of(2020, 3, 1);
        LocalDate end = LocalDate.of(2021, 2, 27);
        Period period = Period.between(start, end);
        System.out.println(period.getYears());   // 0
        System.out.println(period.getMonths());  // 11
        System.out.println(period.getDays());    // 26
    }


    // region time-based
    // 计算程序运行耗时、超时设置、两个时间戳之间的精确间隔。不受日历规则影响（1天总是24小时）

    @Test
    public void test11() {
        Instant start = Instant.parse("2026-08-13T10:00:00Z");
        Instant end =   Instant.parse("2026-08-13T14:30:30Z");

        Duration duration = Duration.between(start, end);
        System.out.println(duration.toHours());   // 4
        System.out.println(duration.toMinutes()); // 270
        System.out.println(duration.getSeconds()); // 16230 (4小时30分30秒)
    }


    // region LocalDateTime 同时支持两者
    // 工程建议
    // 涉及业务日历（如会员有效期、年龄）用 Period
    // 涉及性能指标或超时用 Duration
    // 永远不要用 Duration 去计算“几个月后的日期”，也不要用 Period 去计算“程序运行耗时”。
    // 如果你在项目中需要将两者互相转换（比如把“6年7个月”换算成总天数），由于日历规则不固定，必须指定一个参考日期才能转换。
    @Test
    public void test21() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);

        // Period：忽略时间部分，只算日期差 -> 30天
        Period p = Period.between(start.toLocalDate(), end.toLocalDate());

        // Duration：精确到秒，但注意 Duration.between 对 LocalDateTime 会基于 24小时/天 换算 -> 约 30天23小时59分
        Duration d = Duration.between(start, end);
    }
}
