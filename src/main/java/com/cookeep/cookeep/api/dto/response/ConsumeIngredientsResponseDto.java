package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "ConsumeUserIngredientsResponse",
        description = "유저 식재료 섭취 완료 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class ConsumeIngredientsResponseDto {

    @Schema(
            description = "리워드 정보",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private RewardInfo reward;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RewardInfo {
        @Schema(
                description = "오늘 리워드 지급 여부",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Boolean granted;

        @Schema(
                description = "지급된 리워드 포인트",
                example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Integer points;

        @Schema(
                description = "지급된 쿠키 로그 타입 목록",
                example = "[\"BASIC_DAILY_FIRST_CONSUME\", \"BONUS_URGENT_INGREDIENT_USE\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        private List<CookieLog.CookieLogType> grantedTypes;
    }

    public static ConsumeIngredientsResponseDto of(boolean granted, int points, List<CookieLog.CookieLogType> grantedTypes) {
        return ConsumeIngredientsResponseDto.builder()
                .reward(RewardInfo.builder()
                        .granted(granted)
                        .points(points)
                        .grantedTypes(grantedTypes)
                        .build())
                .build();
    }
}
