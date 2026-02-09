package com.cookeep.cookeep.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "PushNotificationEligibilityResponse",
        description = "푸시 알림 전송 자격 확인 DTO"
)
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PushNotificationEligibilityResponseDto {

    private Boolean eligible;
    private Integer thresholdDays;
    private Integer count;
    private List<IngredientInfo> ingredients;

    @Getter
    @Builder
    public static class IngredientInfo {
        private Long userIngredientId;
        private String name;
        private Integer expiresInDays;
    }

    // eligible이 false일 때 사용하는 정적 팩토리 메서드
    public static PushNotificationEligibilityResponseDto notEligible() {
        return PushNotificationEligibilityResponseDto.builder()
                .eligible(false)
                .build();
    }

    // eligible이 true일 때 사용하는 정적 팩토리 메서드
    public static PushNotificationEligibilityResponseDto eligible(
            int thresholdDays,
            int count,
            List<IngredientInfo> ingredients
    ) {
        return PushNotificationEligibilityResponseDto.builder()
                .eligible(true)
                .thresholdDays(thresholdDays)
                .count(count)
                .ingredients(ingredients)
                .build();
    }
}
