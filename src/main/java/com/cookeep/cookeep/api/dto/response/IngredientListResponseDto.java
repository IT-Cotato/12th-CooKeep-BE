package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.ingredient.common.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "IngredientListResponse",
        description = "카테고리별 식재료 목록 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class IngredientListResponseDto {

    @Schema(
            description = "카테고리별 식재료 그룹 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<CategoryGroup> categories;

    @Schema(
            name = "CategoryGroup",
            description = "카테고리별 식재료 그룹"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CategoryGroup {

        @Schema(
                description = "카테고리",
                example = "VEGETABLE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Category category;

        @Schema(
                description = "카테고리 표시 이름",
                example = "채소",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String displayName;

        @Schema(
                description = "해당 카테고리의 식재료 목록",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private List<IngredientItem> ingredients;

    }

    @Schema(
            name = "IngredientItem",
            description = "개별 식재료 정보"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class IngredientItem {

        @Schema(
                description = "식재료 ID (defaultIngredientId/customIngredientId)",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long id;

        @Schema(
                description = "식재료 타입 (DEFAULT 또는 CUSTOM)",
                example = "DEFAULT",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String type;

        @Schema(
                description = "식재료 이름",
                example = "양파",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String name;

        @Schema(
                description = "식재료 이미지 URL",
                example = "https://s3.amazonaws.com/cookeep/ingredients/onion.png",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String imageUrl;

        @Schema(
                description = "카테고리",
                example = "VEGETABLE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Category category;

    }

}

