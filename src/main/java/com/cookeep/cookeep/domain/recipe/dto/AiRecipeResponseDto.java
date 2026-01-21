package com.cookeep.cookeep.domain.recipe.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiRecipeResponseDto {

    private Long sessionId;

    private Integer changeCount;

    private GeminiRecipeResponseDto recipe;
}
