package com.cookeep.cookeep.api.dto;

import com.cookeep.cookeep.domain.ingredient.common.Category;
import com.cookeep.cookeep.domain.ingredient.common.Storage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomIngredientCreateRequestDto {

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Integer expirationDays;

    @NotNull
    private Storage storage;

    @NotNull
    private Category category;
}
