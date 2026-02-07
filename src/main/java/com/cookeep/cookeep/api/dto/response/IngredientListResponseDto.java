package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.ingredient.common.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class IngredientListResponseDto {

    private List<CategoryGroup> categories;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CategoryGroup {

        private Category category;
        private String displayName;
        private List<IngredientItem> ingredients;

    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class IngredientItem {

        private Long id;

        private String type;

        private String name;

        private String imageUrl;

        private Category category;

    }

}

