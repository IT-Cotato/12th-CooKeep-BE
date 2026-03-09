package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(
        name = "UserIngredientListPreviewResponse",
        description = "식재료 기본 정보 일괄 조회 응답 DTO"
)
public class UserIngredientListPreviewResponseDto {

    @Schema(description = "조회된 식재료 기본 정보 목록")
    private List<UserIngredientPreviewResponseDto> ingredients;

    @Schema(description = "조회된 식재료 수", example = "3")
    private int count;

    public static UserIngredientListPreviewResponseDto of(List<UserIngredientPreviewResponseDto> ingredients) {
        return new UserIngredientListPreviewResponseDto(ingredients, ingredients.size());
    }

}
