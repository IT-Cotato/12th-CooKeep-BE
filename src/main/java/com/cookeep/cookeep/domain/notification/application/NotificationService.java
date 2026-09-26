package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.api.dto.response.GetNotificationResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.notification.dao.NotificationRepository;
import com.cookeep.cookeep.domain.notification.entity.Notification;
import com.cookeep.cookeep.domain.user.application.UserReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 알림함 조회/읽음 처리용
public class NotificationService {

    // 유효기간 - 30일
    private static final int RETENTION_DAYS = 30;

    private final UserReader userReader;
    private final NotificationRepository notificationRepository;

    // 서비스 알림 목록 조회 (최근 30일, 최신순)
    public List<GetNotificationResponseDto> getNotifications(Long userId) {

        userReader.readById(userId);

        LocalDateTime after = LocalDateTime.now().minusDays(RETENTION_DAYS);

        return notificationRepository
                .findAllByUser_UserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, after)
                .stream()
                .map(GetNotificationResponseDto::from)
                .toList();
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUser_UserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }
}
