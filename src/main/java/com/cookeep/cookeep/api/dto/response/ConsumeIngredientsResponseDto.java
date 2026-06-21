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
    private CookieRewardDto reward;

    public static ConsumeIngredientsResponseDto of(
            boolean granted,
            int points,
            List<CookieLog.CookieLogType> types,
            int currentCookieCount) {
        return ConsumeIngredientsResponseDto.builder()
                .reward(CookieRewardDto.builder()
                        .granted(granted)
                        .points(points)
                        .types(types)
                        .currentCookieCount(currentCookieCount)
                        .build())
                .build();
    }
}
