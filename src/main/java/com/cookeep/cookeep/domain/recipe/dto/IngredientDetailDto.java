package com.cookeep.cookeep.domain.recipe.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IngredientDetailDto {

    private String type;
    private Long referenceId;
    private String name;
    private Integer quantity;
    private String unit;
}
