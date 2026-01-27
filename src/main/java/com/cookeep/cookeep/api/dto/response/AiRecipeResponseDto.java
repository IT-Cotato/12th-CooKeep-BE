package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(
        name = "AiRecipeResponse",
        description = "AI 레시피 생성 결과 응답 DTO"
)
@Getter
@Builder
public class AiRecipeResponseDto {

    @Schema(
            description = "AI 레시피 세션 ID",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long sessionId;

    @Schema(
            description = "현재 레시피 변경 횟수 (시도 횟수)",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "1"
    )
    private Integer changeCount;

    @Schema(
            description = "AI가 생성한 레시피 정보",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private GeminiRecipeResponseDto recipe;
}
