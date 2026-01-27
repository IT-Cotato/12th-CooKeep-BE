package com.cookeep.cookeep.domain.recipe.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientSimpleDto {

    @NotNull
    private String type;

    @NotNull
    private Long referenceId;
}
