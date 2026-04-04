package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
            description = "암호화 키 정보",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "keys는 필수입니다.")
    @Valid
    private Keys keys;

    @Schema(name = "WebPushKeys", description = "Web Push 암호화 키")
    @Getter
    @NoArgsConstructor
    public static class Keys {

        @Schema(
                description = "공개키 (암호화용, Base64url)",
                example = "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQt...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "p256dh는 필수입니다.")
        private String p256dh;

        @Schema(
                description = "인증키 (Base64url)",
                example = "tBHItJI5svbpez7KI4CCXg==",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "auth는 필수입니다.")
        private String auth;
    }
}
