package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "WebPushSubscriptionResponse",
        description = "웹 푸시 구독 등록 / 삭제 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class WebPushSubscriptionResponseDto {

    @Schema(description = "처리 결과 메시지", example = "웹 푸시 구독이 등록되었습니다.")
    private String message;

    public static WebPushSubscriptionResponseDto subscribed() {
        return new WebPushSubscriptionResponseDto("웹 푸시 구독이 등록되었습니다.");
    }

    public static WebPushSubscriptionResponseDto unsubscribed() {
        return new WebPushSubscriptionResponseDto("웹 푸시 구독이 해제되었습니다.");
    }
}
