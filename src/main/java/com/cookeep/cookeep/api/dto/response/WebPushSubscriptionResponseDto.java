package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "WebPushSubscriptionResponse",
        description = "웹 푸시 구독 등록 응답 DTO"
)
@Getter
@Builder
public class WebPushSubscriptionResponseDto {

    @Schema(description = "생성된 구독 ID", example = "1")
    private Long subscriptionId;

    @Schema(description = "등록된 엔드포인트", example = "https://fcm.googleapis.com/fcm/send/...")
    private String endpoint;

    @Schema(description = "구독 등록 시각", example = "2026-04-04T12:00:00")
    private LocalDateTime createdAt;

    public static WebPushSubscriptionResponseDto from(WebPushSubscription subscription) {
        return WebPushSubscriptionResponseDto.builder()
                .subscriptionId(subscription.getId())
                .endpoint(subscription.getEndpoint())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
