package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.domain.recipe.entity.Feature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(
        name = "AiRecipeRequest",
        description = "AI 레시피 생성 DTO"
)
@Getter
@NoArgsConstructor
public class AiRecipeRequestDto {

    @Schema(
            description = "AI 레시피 세션 ID (null이면 신규 레시피 생성, 값이 있으면 재요청)",
            example = "10",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Long sessionId;

    @Schema(
            description = "요리 종류 (신규 요청 시 필수)",
            example = "RICE_BOWL"
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            allowableValues = {"SOUP_STEW", "RICE_BOWL", "NOODLE", "STIR_FRY_GRILL", "SALAD_HEALTHY", "SNACK_DESSERT", "ANY"}
    )
    private Feature feature;

    @Schema(
            description = "레시피 생성을 위한 유저 식재료 ID 목록 (신규 요청 시 필수)",
            example = "[1, 2, 3]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<Long> ingredientIds;
}
