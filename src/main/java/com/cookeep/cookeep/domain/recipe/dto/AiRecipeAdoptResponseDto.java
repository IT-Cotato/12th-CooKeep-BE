package com.cookeep.cookeep.domain.recipe.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AiRecipeAdoptResponseDto {

    private Long sessionId;
    private Long recipeId;
    private String message;
    private LocalDateTime completedAt;
}
