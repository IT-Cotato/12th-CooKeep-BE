package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.domain.notification.dao.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Slf4j
@Component
@RequiredArgsConstructor
// 기한 만료 알림 삭제
public class NotificationCleanupScheduler {

    private static final int RETENTION_DAYS = 30;

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        int deletedCount = notificationRepository.deleteAllByCreatedAtBefore(cutoff);

        log.info("=== 30일 경과 알림 삭제 완료. deletedCount={} ===", deletedCount);
    }
}
