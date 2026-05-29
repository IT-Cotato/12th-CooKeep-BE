package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.api.dto.request.WebPushSubscriptionRequestDto;
import com.cookeep.cookeep.api.dto.response.WebPushEligibilityResponseDto;
import com.cookeep.cookeep.api.dto.response.WebPushSubscriptionResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 알림 구독 등록
@Slf4j
@RequiredArgsConstructor
@Service
public class WebPushSubscriptionService {

    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final UserReader userReader;

    // 1. 구독등록: 브라우저가 생성한 Push Subscription 저장 or 이미 존재하면 기존 정보 반환
    @Transactional
    public WebPushSubscriptionResponseDto subscribe(Long userId, WebPushSubscriptionRequestDto request) {

        // request 자체 null
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // endpoint 검증
        if (request.getEndpoint() == null || request.getEndpoint().isBlank()) {
            throw new AppException(ErrorCode.INVALID_ENDPOINT);
        }

        // keys 검증
        if (request.getKeys() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = userReader.readById(userId);

        // 동일 endpoint가 이미 존재하면 해당 정보 응답
        if (webPushSubscriptionRepository.findByEndpoint(request.getEndpoint()).isPresent()) {
            log.debug("WebPush 구독 정보 이미 존재함. userId={}, endpoint={}",
                    userId, maskEndpoint(request.getEndpoint()));
            return WebPushSubscriptionResponseDto.subscribed();
        }

        // 기존 구독 삭제 후 새 구독 등록 (단일 기기 정책 유지하려면)
        webPushSubscriptionRepository.deleteAllByUser_UserId(userId);

        WebPushSubscription subscription = WebPushSubscription.builder()
                .user(user)
                .endpoint(request.getEndpoint())
                .p256dh(request.getKeys().getP256dh())
                .auth(request.getKeys().getAuth())
                .build();

        webPushSubscriptionRepository.save(subscription);

        log.info("WebPush subscription registered. userId={}, subscriptionId={}",
                userId, subscription.getId());

        return WebPushSubscriptionResponseDto.subscribed();
    }

    // 2. 구독 삭제: endpoint 구독 정보 삭제
    @Transactional
    public WebPushSubscriptionResponseDto unsubscribe(Long userId, WebPushSubscriptionRequestDto request) {
        WebPushSubscription subscription = webPushSubscriptionRepository
                .findByEndpoint(request.getEndpoint())
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 본인 소유 여부 검증
        if (!subscription.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        webPushSubscriptionRepository.delete(subscription);

        log.info("WebPush 구독 정보 삭제. userId={}, subscriptionId={}",
                userId, subscription.getId());

        return WebPushSubscriptionResponseDto.unsubscribed();
    }

    // 3. 수신 가능 여부 확인
    @Transactional(readOnly = true)
    public WebPushEligibilityResponseDto checkEligibility(Long userId) {

        userReader.readById(userId);

        boolean subscribed = webPushSubscriptionRepository.existsByUser_UserId(userId);

        return WebPushEligibilityResponseDto.of(subscribed);
    }

    private String maskEndpoint(String endpoint) {
        if (endpoint == null || endpoint.length() < 20) return "****";
        return endpoint.substring(0, 20) + "****";
    }

}
