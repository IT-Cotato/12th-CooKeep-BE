package com.cookeep.cookeep.api.dto.response;

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

    @Schema(description = "온보딩 쿠키 지급 여부", example = "true")
    private boolean ingredientRewardGranted;

    public static UserIngredientListCreateResponseDto of(
            List<UserIngredientCreateResponseDto> ingredients,
            boolean ingredientRewardGranted) {

        return new UserIngredientListCreateResponseDto(
                ingredients,
                ingredients.size(),
                ingredientRewardGranted
        );
    }

}
