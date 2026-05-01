package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PushTestScheduler {
//
//    private final UserIngredientRepository userIngredientRepository;
//    private final WebPushNotificationService webPushNotificationService;
//
//    // 5분마다 실행
//    @Scheduled(cron = "0 0/5 * * * *", zone = "Asia/Seoul")
//    public void sendTestExpirationPush() {
//        log.info("=== [TEST] 강제 푸시 전송 ===");
//
//        Long testUserId = 2L;
//
//        try {
//            webPushNotificationService.sendTestPush(testUserId);
//        } catch (Exception e) {
//            log.error("[TEST] 푸시 전송 실패 userId={}", testUserId, e);
//        }
//    }
//}
