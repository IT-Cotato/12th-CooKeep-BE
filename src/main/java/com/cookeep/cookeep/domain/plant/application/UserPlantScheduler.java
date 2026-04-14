package com.cookeep.cookeep.domain.plant.application;

import com.cookeep.cookeep.domain.notification.application.WebPushNotificationService;
import com.cookeep.cookeep.domain.notification.entity.NotificationType;
import com.cookeep.cookeep.domain.plant.dao.UserPlantRepository;
import com.cookeep.cookeep.domain.plant.entity.PlantStatus;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
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
    private final UserRepository userRepository;
    private final WebPushNotificationService webPushNotificationService;

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

    /**
     * 매일 12:00에 식물 상태(WILTING/FROZEN)인 유저에게 웹 푸시 알림 발송
     * 00:10 plantStatus 갱신 스케줄러 이후 실행되어 최신 상태 반영
     */
    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul")
    public void sendDailyPlantStatusPush() {
        log.info("=== Starting daily plant status push notification ===");

        // 1. WILTING 유저 알림
        List<Long> wiltingUserIds = userRepository
                .findUserIdsByPlantStatusAndMarketingConsent(PlantStatus.WILTING);
        log.info("시듦 알림 대상 유저 수: {}", wiltingUserIds.size());

        for (Long userId : wiltingUserIds) {
            try {
                webPushNotificationService.sendPlantStatusAlert(userId, NotificationType.PLANT_WILTING);
            } catch (Exception e) {
                log.error("시듦 푸시 알림 전송 실패. userId={}, error={}", userId, e.getMessage());
            }
        }

        // 2. FROZEN 유저 알림
        List<Long> frozenUserIds = userRepository
                .findUserIdsByPlantStatusAndMarketingConsent(PlantStatus.FROZEN);
        log.info("성장 정지 알림 대상 유저 수: {}", frozenUserIds.size());

        for (Long userId : frozenUserIds) {
            try {
                webPushNotificationService.sendPlantStatusAlert(userId, NotificationType.PLANT_GROWTH_STOP);
            } catch (Exception e) {
                log.error("성장 정지 푸시 알림 전송 실패. userId={}, error={}", userId, e.getMessage());
            }
        }

        log.info("=== 식물 상태 푸시 알림 전송 완료 ===");
    }
}
