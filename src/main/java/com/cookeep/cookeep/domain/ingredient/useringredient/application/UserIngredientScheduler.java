package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.application.ConsumptionReportService;
import com.cookeep.cookeep.domain.notification.application.WebPushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

// 자정에 모든 식재료 leftDays 필드 업데이트
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIngredientScheduler {

    private final UserIngredientRepository userIngredientRepository;
    private final ConsumptionReportService consumptionReportService;
    private final WebPushNotificationService webPushNotificationService;

    /**
     * 매일 자정(00:00)에 모든 식재료의 남은 일수(leftDays) 업데이트
     * Cron: "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void updateLeftDaysDaily() {
        log.info("=== Starting daily leftDays update ===");

        try {
            // 모든 식재료 조회
            List<UserIngredient> allIngredients = userIngredientRepository.findAll();

            log.info("Total ingredients to update: {}", allIngredients.size());

            // 각 식재료의 leftDays 업데이트
            allIngredients.forEach(UserIngredient::updateLeftDays);

            // 새로 임박된 식재료의 주간 로그 업데이트 (leftDays <= 3)
            LocalDate weekStart = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            List<Long> nearExpiryIds = allIngredients.stream()
                    .filter(ui -> ui.getLeftDays() <= ConsumptionReportService.NEAR_EXPIRY_THRESHOLD)
                    .map(UserIngredient::getIngredientId)
                    .toList();
            if (!nearExpiryIds.isEmpty()) {
                consumptionReportService.updateNearExpiryFlags(nearExpiryIds, weekStart);
                log.info("Updated near-expiry flags for {} ingredients", nearExpiryIds.size());
            }

            log.info("=== Successfully updated leftDays for {} ingredients ===", allIngredients.size());

        } catch (Exception e) {
            log.error("=== Error updating leftDays ===", e);
            // 에러가 발생해도 스케줄러는 계속 실행되어야 함
        }
    }

    @Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
    public void sendDailyExpirationPush() {
        log.info("=== Starting daily expiration push notification ===");

        List<Long> targetUserIds = userIngredientRepository
                .findUserIdsWithExpiringTodayAndMarketingConsent(LocalDate.now());

        log.info("푸시 알림 대상 유저 수: {}", targetUserIds.size());

        for (Long userId : targetUserIds) {
            try {
                webPushNotificationService.sendExpirationAlert(userId);
            } catch (Exception e) {
                log.error("푸시 알림 전송 실패. userId={}, error={}", userId, e.getMessage());
            }
        }

        log.info("=== 푸시 알림 전송 완료 ===");
    }

    /**
     * 테스트용: 매 1분마다 실행 (운영 환경에서는 주석 처리할 것)
     * 개발 중 테스트 용도로만 사용
     */
    // @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    // @Transactional
    // public void updateLeftDaysEveryMinute() {
    //     log.info("=== [TEST] Starting leftDays update ===");
    //     List<UserIngredient> allIngredients = userIngredientRepository.findAll();
    //     allIngredients.forEach(UserIngredient::updateLeftDays);
    //     log.info("=== [TEST] Updated {} ingredients ===", allIngredients.size());
    // }
}
