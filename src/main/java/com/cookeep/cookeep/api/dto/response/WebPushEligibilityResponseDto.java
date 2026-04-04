package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
        name = "WebPushEligibilityResponse",
        description = "웹 푸시 알림 수신 가능 여부 응답 DTO"
)
@Getter
@AllArgsConstructor
public class WebPushEligibilityResponseDto {

    @Schema(
            description = "웹 푸시 수신 가능 여부 (subscription 존재 여부 기준)",
            example = "true"
    )
    private Boolean eligible;

    public static WebPushEligibilityResponseDto of(boolean eligible) {
        return new WebPushEligibilityResponseDto(eligible);
    }

}
