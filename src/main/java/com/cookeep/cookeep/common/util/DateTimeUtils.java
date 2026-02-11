package com.cookeep.cookeep.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

public final class DateTimeUtils {

    private DateTimeUtils() {
        // 인스턴스화 방지
    }

    // 이번 주 월요일 00:00:00 계산
    public static LocalDateTime getStartOfWeek() {
        return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
    }

    // 이번 주 일요일 23:59:59 계산
    public static LocalDateTime getEndOfWeek() {
        return getStartOfWeek().plusDays(7).minusNanos(1);
    }
}
