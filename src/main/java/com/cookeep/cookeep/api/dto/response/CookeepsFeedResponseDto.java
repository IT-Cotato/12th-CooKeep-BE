package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CookeepsFeedResponseDto {
    private Long dailyRecipeId;
    private String title;
    private Integer likeCount;
    private String recipeImageUrl;
    private LocalDateTime createdAt;
}
