package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "쿠킵스 레시피 상세 조회 응답 DTO")
@Getter
@Builder
public class CookeepsRecipeDetailResponseDto {
    @Schema(description = "데일리 레시피 ID")
    private Long dailyRecipeId;

    @Schema(description = "작성자 닉네임")
    private String nickname; // 추가된 필드

    @Schema(description = "레시피 제목")
    private String title;

    @Schema(description = "한줄평")
    private String description;

    @Schema(description = "레시피 내용 (JSON 문자열)")
    @JsonRawValue
    private String content;

    @Schema(description = "요리 사진 URL")
    private String recipeImageUrl;

    @Schema(description = "좋아요 수")
    private Integer likeCount;

    @Schema(description = "등록 시각")
    private LocalDateTime createdAt;

    public static CookeepsRecipeDetailResponseDto from(DailyRecipe dailyRecipe) {
        return CookeepsRecipeDetailResponseDto.builder()
                .dailyRecipeId(dailyRecipe.getId())
                .nickname(dailyRecipe.getUser().getNickname())
                .title(dailyRecipe.getTitle())
                .description(dailyRecipe.getDescription())
                .content(dailyRecipe.getContent())
                .recipeImageUrl(dailyRecipe.getRecipeImageUrl())
                .likeCount(dailyRecipe.getLikeCount())
                .createdAt(dailyRecipe.getCreatedAt())
                .build();
    }
}