package com.cookeep.cookeep.domain.plant.application;

import com.cookeep.cookeep.domain.plant.dao.UserPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPlantScheduler {

    private final UserPlantRepository userPlantRepository;
    private final UserPlantService userPlantService;

    /**
     * 매일 00:10에 현재 식물을 키우는 유저의 plantStatus 갱신
     * 자정(00:00) 식재료 스케줄러와 부하 분산을 위해 10분 뒤 실행
     * Cron: "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void updatePlantStatusDaily() {
        log.info("=== Starting daily plantStatus update ===");

        List<Long> userIds = userPlantRepository.findUserIdsWithGrowingPlant();
        log.info("Total users with growing plant: {}", userIds.size());

        int successCount = 0;
        for (Long userId : userIds) {
            try {
                userPlantService.checkAndUpdatePlantStatusById(userId);
                successCount++;
            } catch (Exception e) {
                log.error("plantStatus 업데이트 실패. userId={}, error={}", userId, e.getMessage());
            }
        }

        log.info("=== Successfully updated plantStatus for {}/{} users ===", successCount, userIds.size());
    }
}
