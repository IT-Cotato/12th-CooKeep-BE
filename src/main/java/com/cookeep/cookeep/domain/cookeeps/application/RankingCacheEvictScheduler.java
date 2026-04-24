package com.cookeep.cookeep.domain.cookeeps.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingCacheEvictScheduler {

    private final RankingCacheService rankingCacheService;

    @Scheduled(cron = "0 1 0 1 * *", zone = "Asia/Seoul")
    public void evictWateringRankingCache() {
        rankingCacheService.evictWateringRankingCache();
    }

    @Scheduled(cron = "0 1 0 * * MON", zone = "Asia/Seoul")
    public void evictRecipeRankingCache() {
        rankingCacheService.evictRecipeRankingCache();
    }
}
