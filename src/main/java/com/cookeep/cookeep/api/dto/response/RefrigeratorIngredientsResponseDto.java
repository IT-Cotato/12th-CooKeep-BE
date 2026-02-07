package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "RefrigeratorIngredientsResponse",
        description = "냉장고 식재료 목록 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class RefrigeratorIngredientsResponseDto {

    @Schema(
            description = "냉장 보관 식재료 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<IngredientItem> fridge;

    @Schema(
            description = "냉동 보관 식재료 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<IngredientItem> freezer;

    @Schema(
            description = "실온 보관 식재료 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<IngredientItem> pantry;

    @Schema(
            name = "IngredientItem",
            description = "식재료 항목 (냉장고 탭에서 보이는 정보)"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class IngredientItem {

        @Schema(
                description = "유저 식재료 ID",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long ingredientId;

        @Schema(
                description = "식재료 이름",
                example = "양파",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String name;

        @Schema(
                description = "남은 일수 (D-day)",
                example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Integer leftDays;

        @Schema(
                description = "식재료 이미지 URL",
                example = "https://s3.amazonaws.com/cookeep/ingredients/carrot.png",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String imageUrl;
    }
}
