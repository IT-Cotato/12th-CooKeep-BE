package com.cookeep.cookeep.api.dto.response;

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
}
