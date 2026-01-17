package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.domain.ingredient.common.Category;
import com.cookeep.cookeep.domain.ingredient.common.Storage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(
        name = "CustomIngredientCreateRequest",
        description = "커스텀 식재료 생성 요청 DTO"
)
public class CustomIngredientCreateRequestDto {

    @Schema(
            description = "커스텀 식재료 이름",
            example = "두쫀쿠",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    private String name;

    @NotNull
    @Positive
    @Schema(
            description = "기본 보관 가능 기간 (일)",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer expirationDays;

    @NotNull
    @Schema(
            description = "보관 장소",
            example = "FRIDGE",
            allowableValues = {"FRIDGE", "FREEZER", "PANTRY"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Storage storage;

    @NotNull
    @Schema(
            description = "식재료 카테고리",
            example = "PROCESSED",
            allowableValues = {"VEGETABLE", "FRUIT", "MEAT", "SEAFOOD", "DAIRY_EGG", "GRAIN_RICE_NOODLE", "BAKERY", "SEASONING_SAUCE", "READY_MEAL", "SNACK_DESSERT", "BEVERAGE", "FERMENTED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Category category;
}
