package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushTestScheduler {

    private final UserIngredientRepository userIngredientRepository;
    private final WebPushNotificationService webPushNotificationService;

    // 5분마다 실행
    @Scheduled(cron = "0 0/5 * * * *", zone = "Asia/Seoul")
    public void sendTestExpirationPush() {
        log.info("=== [TEST] 유통기한 임박 푸시 알림 테스트 실행 ===");

        List<Long> targetUserIds = userIngredientRepository
                .findUserIdsWithExpiringTodayAndMarketingConsent(LocalDate.now());

        for (Long userId : targetUserIds) {
            try {
                webPushNotificationService.sendExpirationAlert(userId);
            } catch (Exception e) {
                log.error("[TEST] 푸시 알림 전송 실패. userId={}", userId, e);
            }
        }
    }
}
