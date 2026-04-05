package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.api.dto.response.WebPushSendResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.notification.entity.NotificationType;
import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// 웹 푸시 알림 전송
@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushNotificationService {

    private final UserReader userReader;
    private final UserIngredientRepository userIngredientRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final PushService pushService;

    public WebPushSendResponseDto sendExpirationAlert(Long userId) {

        User user = userReader.readById(userId);

        // 1. 마케팅 수신 동의 확인
        if (!Boolean.TRUE.equals(user.getMarketingConsent())) {
            log.debug("웹 푸시 알림 미전송 - 수신 미동의. userId={}", userId);
            return WebPushSendResponseDto.notConsented();
        }

        // 2. leftDays = 0 인 식재료 존재 여부 확인
        LocalDate today = LocalDate.now();
        boolean hasExpiringToday =
                userIngredientRepository.existsByUserIdAndExpirationDate(userId, today);

        if (!hasExpiringToday) {
            log.debug("웹 푸시 알림 미전송 - 만료 임박 재료 없음. userId={}", userId);
            return WebPushSendResponseDto.noExpiringIngredients();
        }

        // 3. 구독 정보 조회
        List<WebPushSubscription> subscriptions =
                webPushSubscriptionRepository.findAllByUser_UserId(userId);

        if (subscriptions.isEmpty()) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        // 4. 알림 페이로드 생성
        String payload = buildPayload(NotificationType.EXPIRATION);

        // 5. 구독별 전송 (실패 구독은 제거)
        sendToSubscriptions(subscriptions, payload);

        log.info("웹 푸시 알림 전송 완료. userId={}, subscriptionCount={}", userId, subscriptions.size());

        return WebPushSendResponseDto.sent();

    }

    private String buildPayload(NotificationType type) {
        JSONObject payload = new JSONObject();

        payload.put("title", tyoe.getTitle());
        payload.put("body", "오늘 유통기한이 만료되는 재료가 있어요!");
        payload.put("url", "/refrigerator");
        payload.put("type", type.name());
        return payload.toString();
    }

    // 각 구독에 푸시 알림을 전송
    // 전송 실패 시 410 Gone / 404 Not Found 응답이면 만료 구독으로 판단하고 DB에서 삭제
    private void sendToSubscriptions(List<WebPushSubscription> subscriptions, String payload) {
        for (WebPushSubscription subscription : subscriptions) {
            try {
                Notification notification = new Notification(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        payload.getBytes()
                );

                int statusCode = pushService.send(notification).getStatusLine().getStatusCode();

                // 410 Gone 또는 404 Not Found : 만료된 구독 → 삭제
                if (statusCode == 410 || statusCode == 404) {
                    log.warn("만료된 구독 삭제. subscriptionId={}, statusCode={}",
                            subscription.getId(), statusCode);
                    webPushSubscriptionRepository.delete(subscription);
                }

            } catch (Exception e) {
                log.error("웹 푸시 전송 실패. subscriptionId={}, error={}",
                        subscription.getId(), e.getMessage());
                // 개별 구독 실패는 전체 흐름을 중단하지 않음
            }
        }
    }
}
