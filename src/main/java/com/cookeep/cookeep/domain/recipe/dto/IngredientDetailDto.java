package com.cookeep.cookeep.domain.recipe.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class IngredientDetailDto {

    @NotNull
    private String type;

    @NotNull
    private Long referenceId;

    @NotNull
    private String name;

    @NotNull
    private Integer quantity;

    @NotNull
    private String unit;
}
