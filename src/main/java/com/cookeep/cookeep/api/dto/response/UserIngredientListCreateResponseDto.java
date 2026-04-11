package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(name = "UserIngredientListCreateResponse", description = "유저 식재료 일괄 등록 응답 DTO")
public class UserIngredientListCreateResponseDto {

    @Schema(description = "등록된 식재료 목록")
    private List<UserIngredientCreateResponseDto> ingredients;

    @Schema(description = "등록된 식재료 수", example = "4")
    private int count;

    @Schema(description = "온보딩 쿠키 지급 여부", example = "true")
    private boolean ingredientRewardGranted;

    @Schema(description = "이번 등록으로 지급된 리워드 정보")
    private RewardInfo reward;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RewardInfo {
        @Schema(description = "리워드 지급 여부", example = "true")
        private Boolean granted;

        @Schema(description = "지급된 총 쿠키 포인트", example = "1")
        private Integer points;

        @Schema(description = "지급된 쿠키 로그 타입 목록", example = "[\"ONBOARDING_INGREDIENT\"]")
        private List<CookieLog.CookieLogType> grantedTypes;
    }

    public static UserIngredientListCreateResponseDto of(
            List<UserIngredientCreateResponseDto> ingredients,
            boolean ingredientRewardGranted) {

        List<CookieLog.CookieLogType> grantedTypes = ingredientRewardGranted
                ? List.of(CookieLog.CookieLogType.ONBOARDING_INGREDIENT)
                : List.of();

        RewardInfo rewardInfo = RewardInfo.builder()
                .granted(ingredientRewardGranted)
                .points(ingredientRewardGranted ? CookieLog.CookieLogType.ONBOARDING_INGREDIENT.getDefaultAmount() : 0)
                .grantedTypes(grantedTypes)
                .build();

        return new UserIngredientListCreateResponseDto(
                ingredients,
                ingredients.size(),
                ingredientRewardGranted,
                rewardInfo
        );
    }

}
