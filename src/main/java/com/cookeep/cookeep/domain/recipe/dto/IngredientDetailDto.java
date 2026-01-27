package com.cookeep.cookeep.domain.recipe.dto;

import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
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
    private String type; // DEFAULT or CUSTOM

    @NotNull
    private Long referenceId; // default_ingredients.id or custom_ingredients.id

    private String name;    // 조회해서 채움
    private Integer quantity; // AI가 생성
    private String unit;    // user_ingredients에서 조회

    public static IngredientDetailDto from(UserIngredient entity) {
        return IngredientDetailDto.builder()
                .type(entity.getType().name())
                .referenceId(entity.getReferenceId())
                .name(entity.getMemo())
                .quantity(entity.getQuantity())
                .unit(entity.getUnit().name())
                .build();
    }

}
