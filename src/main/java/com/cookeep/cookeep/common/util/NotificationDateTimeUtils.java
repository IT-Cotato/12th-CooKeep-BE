package com.cookeep.cookeep.common.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class NotificationDateTimeUtils {

    // 알림함(공지사항/서비스 알림)에 노출되는 기간 - 30일 경과 시 자동 삭제됨
    private static final long INBOX_RETENTION_DAYS = 30;

    private static final DateTimeFormatter ABSOLUTE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d.");

    private NotificationDateTimeUtils() {
        // 인스턴스화 방지
    }

    // 알림함(공지사항/서비스 알림)에 노출할 경과 시간 텍스트를 계산함
    // 방금 전 : 0초 이상 1분 미만
    // N분 전  : 1분 이상 1시간 미만 (내림)
    // N시간 전 : 1시간 이상 24시간 미만 (내림)
    // N일 전  : 1일 이상 7일 미만
    // 절대 날짜(YYYY. M. D.) : 7일 이상 30일 이내
    // 30일 초과 : 알림함에서 노출되지 않아야 하는 데이터이므로 null 반환 (방어 로직)
    public static String getElapsedTimeText(LocalDateTime createdAt) {
        if (createdAt == null) {
            return null;
        }

        Duration duration = Duration.between(createdAt, LocalDateTime.now());

        if (duration.isNegative()) {
            // 미래 시각이 들어온 경우(서버 시간 오차 등)에 대한 방어
            return "방금 전";
        }

        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return "방금 전";
        }

        if (minutes < 60) {
            return minutes + "분 전";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "시간 전";
        }

        long days = duration.toDays();
        if (days < 7) {
            return days + "일 전";
        }

        if (days <= INBOX_RETENTION_DAYS) {
            return createdAt.format(ABSOLUTE_DATE_FORMATTER);
        }

        // 30일 초과 - 알림함 조회 쿼리에서 이미 걸러져야 하는 케이스
        return null;
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
