package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.ingredient.common.domain.Category;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(
        name = "CustomIngredientCreateResponse",
        description = "커스텀 식재료 생성 응답 DTO"
)
public class CustomIngredientCreateResponseDto {

    @Schema(
            description = "생성된 커스텀 식재료 ID",
            example = "3"
    )
    private Long customIngredientId;

    @Schema(
            description = "커스텀 식재료 이름",
            example = "킹스베리딸기"
    )
    private String name;

    @Schema(
            description = "기본 보관 가능 기간 (일)",
            example = "3"
    )
    private Integer expirationDays;

    @Schema(
            description = "보관 장소",
            example = "FRIDGE"
    )
    private Storage storage;

    @Schema(
            description = "식재료 카테고리",
            example = "FRUIT"
    )
    private Category category;

    @Schema(description = "식재료 이미지 URL", example = "https://s3.amazonaws.com/cookeep/ingredients/default.png")
    private String imageUrl;

    public static CustomIngredientCreateResponseDto from(CustomIngredient ingredient) {
        return new CustomIngredientCreateResponseDto(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getExpirationDays(),
                ingredient.getStorage(),
                ingredient.getCategory(),
                ingredient.getImageUrl()
        );
    }
}
