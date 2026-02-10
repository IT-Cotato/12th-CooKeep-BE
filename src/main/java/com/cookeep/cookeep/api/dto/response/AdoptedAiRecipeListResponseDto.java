package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "AdoptedAiRecipeListResponse",
        description = "채택된 AI 레시피 목록 항목 DTO (레시피 선택 화면)"
)
@Getter
@Builder
public class AdoptedAiRecipeListResponseDto {

    @Schema(description = "AI 레시피 ID", example = "1")
    private Long aiRecipeId;

    @Schema(description = "레시피 제목", example = "고추장 마요 달걀밥")
    private String title;

    @Schema(description = "즐겨찾기 여부", example = "true")
    private Boolean isPinned;

    @Schema(description = "채택 시각", example = "2026-02-07T14:30:00")
    private LocalDateTime createdAt;

    public static AdoptedAiRecipeListResponseDto from(AiRecipe aiRecipe) {
        return AdoptedAiRecipeListResponseDto.builder()
                .aiRecipeId(aiRecipe.getId())
                .title(aiRecipe.getTitle())
                .isPinned(aiRecipe.getSession().getIsPinned())
                .createdAt(aiRecipe.getCreatedAt())
                .build();
    }
}
