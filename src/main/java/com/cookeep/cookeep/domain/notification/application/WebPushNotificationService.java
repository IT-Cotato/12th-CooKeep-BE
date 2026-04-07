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

        // 2. D-0 재료 존재 여부 확인
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

        // 5. 구독별 전송 — 실제로 브라우저에 전달된 횟수를 반환
        int successCount = sendToSubscriptions(subscriptions, payload);

        log.info("웹 푸시 알림 전송 완료. userId={}, total={}, success={}",
                userId, subscriptions.size(), successCount);

        // 6. 성공 횟수가 0이면 모든 구독이 만료됐거나 전송에 실패한 것
        return successCount > 0
                ? WebPushSendResponseDto.sent(NotificationType.EXPIRATION)
                : WebPushSendResponseDto.allSubscriptionsExpired();

    }

    // --- 내부 메서드 ---

    // 알림 내용 생성
    private String buildPayload(NotificationType type) {
        JSONObject payload = new JSONObject();

        payload.put("title", type.getTitle());
        payload.put("body", type.getBody());
        payload.put("url", type.getUrl());
        payload.put("type", type.name());
        return payload.toString();
    }

    // 각 구독에 푸시 알림을 전송
    // 전송 실패 시 410 Gone / 404 Not Found 응답이면 만료 구독으로 판단하고 DB에서 삭제
    private int sendToSubscriptions(List<WebPushSubscription> subscriptions, String payload) {

        int successCount = 0;

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
                    // 만료된 구독 → 삭제, 성공 카운트에 포함하지 않음
                    log.warn("만료된 구독 삭제. subscriptionId={}, statusCode={}",
                            subscription.getId(), statusCode);
                    webPushSubscriptionRepository.delete(subscription);

                } else if (statusCode >= 200 && statusCode < 300) {
                    // 2xx → 브라우저에 실제로 전달됨
                    successCount++;

                } else {
                    log.warn("웹 푸시 비정상 응답. subscriptionId={}, statusCode={}",
                            subscription.getId(), statusCode);
                }

            } catch (Exception e) {
                log.error("웹 푸시 전송 실패. subscriptionId={}, error={}",
                        subscription.getId(), e.getMessage());
                // 개별 구독 실패는 전체 흐름을 중단하지 않음
            }
        }
        return successCount;

    }
}
