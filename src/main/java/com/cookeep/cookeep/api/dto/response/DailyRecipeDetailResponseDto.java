package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "DailyRecipeDetailResponse",
        description = "데일리 레시피 상세 조회 응답 DTO"
)
@Getter
@Builder
public class DailyRecipeDetailResponseDto {

    @Schema(description = "데일리 레시피 ID", example = "1")
    private Long dailyRecipeId;

    @Schema(description = "레시피 제목", example = "고추장 마요 달걀밥")
    private String title;

    @Schema(description = "한줄평", example = "버터가 다 녹고 프라이를 올리는 게 포인트")
    private String description;

    @Schema(description = "레시피 내용 (AI 레시피 스냅샷, JSON 문자열)")
    private String content;

    @Schema(description = "요리 사진 URL")
    private String recipeImageUrl;

    @Schema(description = "공개 여부", example = "false")
    private Boolean isPublic;

    @Schema(description = "좋아요 수", example = "0")
    private Integer likeCount;

    @Schema(description = "원본 AI 레시피 ID", example = "1")
    private Long aiRecipeId;

    @Schema(description = "등록 시각", example = "2026-02-07T14:30:00")
    private LocalDateTime createdAt;

    public static DailyRecipeDetailResponseDto from(DailyRecipe dailyRecipe) {
        return DailyRecipeDetailResponseDto.builder()
                .dailyRecipeId(dailyRecipe.getId())
                .title(dailyRecipe.getTitle())
                .description(dailyRecipe.getDescription())
                .content(dailyRecipe.getContent())
                .recipeImageUrl(dailyRecipe.getRecipeImageUrl())
                .isPublic(dailyRecipe.getIsPublic())
                .likeCount(dailyRecipe.getLikeCount())
                .aiRecipeId(dailyRecipe.getAiRecipe().getId())
                .createdAt(dailyRecipe.getCreatedAt())
                .build();
    }
}
