package com.cookeep.cookeep.domain.cookeeps.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingCacheEvictScheduler {

    private final RankingCacheService rankingCacheService;

    @Scheduled(cron = "0 1 0 1 * *", zone = "Asia/Seoul")
    public void evictOnMonthStart() {
        rankingCacheService.evictAllRankingCaches();
    }

    @Scheduled(cron = "0 1 0 * * MON", zone = "Asia/Seoul")
    public void evictOnWeekStart() {
        rankingCacheService.evictAllRankingCaches();
    }
}
