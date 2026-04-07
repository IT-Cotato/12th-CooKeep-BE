package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
        name = "WebPushSendResponse",
        description = "웹 푸시 알림 전송 결과 응답 DTO"
)
@Getter
@AllArgsConstructor
public class WebPushSendResponseDto {

    @Schema(description = "알림 전송 여부", example = "true")
    private Boolean sent;

    @Schema(description = "결과 메시지", example = "유통기한 만료 재료 알림이 전송되었습니다.")
    private String message;

    public static WebPushSendResponseDto sent(NotificationType type) {
        return new WebPushSendResponseDto(true,
                type.getDisplayName() + " 알림이 전송되었습니다.");
    }


    public static WebPushSendResponseDto notConsented() {
        return new WebPushSendResponseDto(false, "알림 수신 동의가 되어있지 않습니다.");
    }

    public static WebPushSendResponseDto noExpiringIngredients() {
        return new WebPushSendResponseDto(false, "유통기한이 만료된 재료가 없습니다.");
    }

    // 구독은 존재. 전부 만료(410/404) or 전송 실패
    public static WebPushSendResponseDto allSubscriptionsExpired() {
        return new WebPushSendResponseDto(false, "유효한 구독이 없어 알림을 전송하지 못했습니다.");
    }

}
