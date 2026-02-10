package com.cookeep.cookeep.domain.recipe.dto;

import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "IngredientDetail",
        description = "AI 레시피 처리 과정에서 사용되는 상세 재료 DTO"
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientDetailDto {

    @Schema(
            description = "유저 식재료 ID (user_ingredients.ingredients_id)",
            example = "7",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long ingredientId;

    @Schema(
            description = "재료 이름 (서버에서 조회하여 채움)",
            example = "양파",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String name;

    @Schema(
            description = "재료 수량 (AI가 생성)",
            example = "2",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "1"
    )
    private Integer quantity;

    @Schema(
            description = "재료 단위 (user_ingredients에서 조회)",
            example = "PIECE",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String unit;

}
