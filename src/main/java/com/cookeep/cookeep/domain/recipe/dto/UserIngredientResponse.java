package com.cookeep.cookeep.domain.recipe.dto;

import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.common.Unit;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserIngredientResponse {
    private String name;
    private int quantity;
    private Unit unit;
    private Type type;
    private Long referenceId;
}
