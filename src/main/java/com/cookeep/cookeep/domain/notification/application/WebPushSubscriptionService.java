package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.api.dto.request.WebPushSubscriptionRequestDto;
import com.cookeep.cookeep.api.dto.response.WebPushSubscriptionResponseDto;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class WebPushSubscriptionService {

    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final UserReader userReader;

    // 1. 구독등록: 브라우저가 생성한 Push Subscription 저장 or 이미 존재하면 기존 정보 반환
    @Transactional
    public WebPushSubscriptionResponseDto subscribe(Long userId, WebPushSubscriptionRequestDto request) {
        User user = userReader.readById(userId);

        // 동일 endpoint가 이미 존재하면 기존 것 반환 (멱등)
        return webPushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .map(existing -> {
                    log.debug("WebPush subscription already exists. userId={}, endpoint={}",
                            userId, maskEndpoint(request.getEndpoint()));
                    return WebPushSubscriptionResponseDto.from(existing);
                })
                .orElseGet(() -> {
                    WebPushSubscription subscription = WebPushSubscription.builder()
                            .user(user)
                            .endpoint(request.getEndpoint())
                            .p256dh(request.getP256dh())
                            .auth(request.getAuth())
                            .build();

                    WebPushSubscription saved = webPushSubscriptionRepository.save(subscription);
                    log.info("WebPush subscription registered. userId={}, subscriptionId={}",
                            userId, saved.getId());
                    return WebPushSubscriptionResponseDto.from(saved);
                });
    }

}
