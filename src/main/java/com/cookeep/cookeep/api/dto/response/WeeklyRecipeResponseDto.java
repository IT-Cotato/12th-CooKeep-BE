package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeeklyRecipeResponseDto {
    private int rank;
    private Long dailyRecipeId;
    private String title;
    private Integer likeCount;
    private String recipeImageUrl;
}
