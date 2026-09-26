package com.cookeep.cookeep.domain.notification.dao;

import com.cookeep.cookeep.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 유저의 최근 N일 이내 알림을 최신순으로 조회 (알림함 - 서비스 알림 탭)
    List<Notification> findAllByUser_UserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime after);

    // 읽음 처리 시 본인 소유 여부 검증을 위한 조회
    Optional<Notification> findByNotificationIdAndUser_UserId(Long notificationId, Long userId);

    // 기준 시각 이전에 생성된 알림 일괄 삭제 (30일 경과 알림 자동 삭제 스케줄러용)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    int deleteAllByCreatedAtBefore(@Param("before") LocalDateTime before);
}
