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
public class IngredientDetailDto {

    @NotNull
    private String type;

    @NotNull
    private Long referenceId;

    private String name;

    @NotNull
    private Integer quantity;

    @NotNull
    private String unit;
}
