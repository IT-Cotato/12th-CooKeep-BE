package com.cookeep.cookeep.domain.notification.dao;

import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    // 유저별 모든 구독 정보 조히
    List<WebPushSubscription> findAllByUser_UserId(Long userId);

    // 엔드포인트 기준 조회
    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    // 유저 + 엔드포인트 별 조회
    Optional<WebPushSubscription> findByUser_UserIdAndEndpoint(Long userId, String endpoint);

    // 엔드포인트 별 존재 여부 확인
    boolean existsByEndpoint(String endpoint);

    // 유저 별 구독 존재 여부 확인
    boolean existsByUser_UserId(Long userId);

    // 엔드포인트 삭제
    void deleteByEndpoint(String endpoint);

    // 특정 유저의 모든 구독 정보 삭제
    void deleteAllByUser_UserId(Long userId);
}
