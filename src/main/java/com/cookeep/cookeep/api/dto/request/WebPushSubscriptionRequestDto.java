package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "WebPushSubscriptionRequest",
        description = "웹 푸시 구독 등록 요청 DTO"
)
@Getter
@NoArgsConstructor
public class WebPushSubscriptionRequestDto {

    @Schema(
            description = "브라우저가 발급한 Push 엔드포인트 URL",
            example = "https://fcm.googleapis.com/fcm/send/...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "endpoint는 필수입니다.")
    private String endpoint;

    @Schema(
            description = "VAPID 공개 키 (Base64url)",
            example = "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQtUbVlO...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "p256dh는 필수입니다.")
    private String p256dh;

    @Schema(
            description = "VAPID 인증 시크릿 (Base64url)",
            example = "tBHItJI5svbpez7KI4CCXg==",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "auth는 필수입니다.")
    private String auth;
}
