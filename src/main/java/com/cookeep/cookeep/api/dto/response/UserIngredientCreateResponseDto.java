package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserIngredientCreateResponseDto {

    private String type;
    private Long ingredientId;
    private Long referenceId;
    private String name;
    private Integer quantity;
    private String unit;
    private String storage;
    private LocalDate expirationDate;
    private Integer leftDays;
    private String memo;

    public static UserIngredientCreateResponseDto of(
            UserIngredient userIngredient,
            String ingredientName
    ) {
        return new UserIngredientCreateResponseDto(
                userIngredient.getType().name(),
                userIngredient.getIngredientId(),
                userIngredient.getReferenceId(),
                ingredientName,
                userIngredient.getQuantity(),
                userIngredient.getUnit().name(),
                userIngredient.getStorage().name(),
                userIngredient.getExpirationDate(),
                userIngredient.getLeftDays(),
                userIngredient.getMemo()
        );
    }
}
