package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "DailyRecipeListResponse",
        description = "데일리 레시피 목록 항목 응답 DTO"
)
@Getter
@Builder
public class DailyRecipeListResponseDto {

    @Schema(description = "데일리 레시피 ID", example = "1")
    private Long dailyRecipeId;

    @Schema(description = "레시피 제목", example = "고추장 마요 달걀밥")
    private String title;

    @Schema(description = "요리 사진 URL")
    private String recipeImageUrl;

    @Schema(description = "공개 여부", example = "false")
    private Boolean isPublic;

    @Schema(description = "등록 시각", example = "2026-02-07T14:30:00")
    private LocalDateTime createdAt;

    public static DailyRecipeListResponseDto from(DailyRecipe dailyRecipe) {
        return DailyRecipeListResponseDto.builder()
                .dailyRecipeId(dailyRecipe.getId())
                .title(dailyRecipe.getTitle())
                .recipeImageUrl(dailyRecipe.getRecipeImageUrl())
                .isPublic(dailyRecipe.getIsPublic())
                .createdAt(dailyRecipe.getCreatedAt())
                .build();
    }
}
