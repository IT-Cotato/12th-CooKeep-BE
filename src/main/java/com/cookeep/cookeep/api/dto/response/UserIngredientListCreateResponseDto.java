package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
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

    @Schema(description = "이번 등록으로 지급된 리워드 정보")
    private CookieRewardDto reward;

    public static UserIngredientListCreateResponseDto of(
            List<UserIngredientCreateResponseDto> ingredients,
            boolean ingredientRewardGranted,
            int currentCookieCount) {

        List<CookieLog.CookieLogType> types = ingredientRewardGranted
                ? List.of(CookieLog.CookieLogType.ONBOARDING_INGREDIENT)
                : List.of();

        CookieRewardDto reward = CookieRewardDto.builder()
                .granted(ingredientRewardGranted)
                .points(ingredientRewardGranted ? CookieLog.CookieLogType.ONBOARDING_INGREDIENT.getDefaultAmount() : 0)
                .types(types)
                .currentCookieCount(currentCookieCount)
                .build();

        return new UserIngredientListCreateResponseDto(ingredients, ingredients.size(), reward);
    }
}
