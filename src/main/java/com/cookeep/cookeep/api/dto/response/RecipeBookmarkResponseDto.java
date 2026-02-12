package com.cookeep.cookeep.api.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeBookmarkResponseDto {
    private Long dailyRecipeId;
    private boolean isBookmarked;

    public static RecipeBookmarkResponseDto from(Long dailyRecipeId, boolean isBookmarked) {
        return RecipeBookmarkResponseDto.builder()
                .dailyRecipeId(dailyRecipeId)
                .isBookmarked(isBookmarked)
                .build();
    }
}
