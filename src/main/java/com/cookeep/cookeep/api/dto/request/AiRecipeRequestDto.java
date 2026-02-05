package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.domain.recipe.dto.IngredientSimpleDto;
import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
import com.cookeep.cookeep.domain.recipe.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
            description = "레시피 난이도 (신규 요청 시 필수)",
            example = "EASY",
            allowableValues = {"EASY", "NORMAL", "HARD"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Difficulty difficulty;

    @Schema(
            description = "레시피 생성을 위한 재료 목록 (신규 요청 시 필수)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<IngredientSimpleDto> ingredients;
}
