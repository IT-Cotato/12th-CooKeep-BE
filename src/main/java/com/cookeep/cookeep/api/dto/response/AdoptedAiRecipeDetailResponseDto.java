package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "AdoptedAiRecipeDetailResponse",
        description = "채택된 AI 레시피 상세 조회 응답 DTO"
)
@Getter
@Builder
public class AdoptedAiRecipeDetailResponseDto {

    @Schema(description = "AI 레시피 ID", example = "1")
    private Long aiRecipeId;

    @Schema(description = "레시피 제목", example = "고추장 마요 달걀밥")
    private String title;

    @Schema(description = "재료 정보 (JSON)")
    private String ingredientsJson;

    @Schema(description = "조리 단계 (JSON)")
    private String stepsJson;

    @Schema(description = "참고 유튜브 영상 (JSON)")
    private String youtubeUrlJson;

    @Schema(description = "채택 시각", example = "2026-02-07T14:30:00")
    private LocalDateTime createdAt;

    public static AdoptedAiRecipeDetailResponseDto from(AiRecipe aiRecipe) {
        return AdoptedAiRecipeDetailResponseDto.builder()
                .aiRecipeId(aiRecipe.getId())
                .title(aiRecipe.getTitle())
                .ingredientsJson(aiRecipe.getIngredientsJson())
                .stepsJson(aiRecipe.getStepsJson())
                .youtubeUrlJson(aiRecipe.getYoutubeUrlJson())
                .createdAt(aiRecipe.getCreatedAt())
                .build();
    }
}
