package com.cookeep.cookeep.domain.recipe.dto;

import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
import com.cookeep.cookeep.domain.recipe.entity.MessageType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiRecipeRequestDto {

    private Long sessionId;

    private MessageType messageType;

    private Difficulty difficulty;

    private List<IngredientSimpleDto> ingredients;
}
