package com.cookeep.cookeep.api.dto;

import com.cookeep.cookeep.domain.ingredient.common.Category;
import com.cookeep.cookeep.domain.ingredient.common.Storage;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomIngredientCreateResponseDto {


    private Long customIngredientId;
    private String name;
    private Integer expirationDays;
    private Storage storage;
    private Category category;

    public static CustomIngredientCreateResponseDto from(CustomIngredient ingredient) {
        return new CustomIngredientCreateResponseDto(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getExpirationDays(),
                ingredient.getStorage(),
                ingredient.getCategory()
        );
    }
}
