package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.common.util.NotificationDateTimeUtils;
import com.cookeep.cookeep.domain.notification.entity.Notification;
import com.cookeep.cookeep.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "GetNotificationResponse",
        description = "알림함 - 서비스 알림 목록 조회 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class GetNotificationResponseDto {

    @Schema(description = "알림 ID", example = "15")
    private Long notificationId;

    @Schema(description = "알림 유형", example = "EXPIRATION")
    private NotificationType type;

    @Schema(description = "알림 제목", example = "유통기한임박")
    private String title;

    @Schema(description = "알림 본문", example = "오늘 유통기한이 만료되는 재료가 있어요! \n 지금 확인하고 요리해볼까요?")
    private String body;

    @Schema(description = "클릭 시 이동할 경로", example = "/fridge")
    private String url;

    @Schema(description = "읽음 여부", example = "false")
    private boolean isRead;

    @Schema(description = "알림 생성 시각", example = "2026-09-08T09:41:00")
    private LocalDateTime createdAt;

    @Schema(description = "경과 시간 (방금 전 / n분 / n시간/ n일 전)", example = "방금 전")
    private String elapsedTime;

    public static GetNotificationResponseDto from(Notification notification) {
        return GetNotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .url(notification.getUrl())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .elapsedTime(NotificationDateTimeUtils.getElapsedTimeText(notification.getCreatedAt()))
                .build();
    }

}
