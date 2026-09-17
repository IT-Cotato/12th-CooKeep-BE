package com.cookeep.cookeep.domain.notice.application;

import com.cookeep.cookeep.domain.notice.dao.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeCleanupScheduler {

    private static final int RETENTION_DAYS = 300;

    private final NoticeRepository noticeRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredNotices() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        int deletedCount = noticeRepository.deleteAllByCreatedAtBefore(cutoff);

        log.info("=== 30일 경과 공지사항 삭제 완료. deletedCount={} ===", deletedCount);
    }
}
