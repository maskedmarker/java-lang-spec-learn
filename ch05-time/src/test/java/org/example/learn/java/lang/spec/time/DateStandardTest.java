package org.example.learn.java.lang.spec.time;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DateStandardTest {

    @Test
    public void test0() {
        // ISO 8601 日期
        LocalDate date = LocalDate.parse("2024-06-27");
        System.out.println("Date: " + date);

        // ISO 8601 日期和时间
        LocalDateTime dateTime = LocalDateTime.parse("2024-06-27T14:23:45");
        System.out.println("DateTime: " + dateTime);

        // ISO 8601 日期和时间 (带时区偏移)
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2024-06-27T14:23:45+02:00");
        System.out.println("OffsetDateTime: " + offsetDateTime);

        // 格式化为 ISO 8601
        String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        System.out.println("Formatted Date: " + formattedDate);

        String formattedDateTime = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println("Formatted DateTime: " + formattedDateTime);

        String formattedOffsetDateTime = offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        System.out.println("Formatted OffsetDateTime: " + formattedOffsetDateTime);
    }
}
