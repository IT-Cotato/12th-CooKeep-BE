package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "IngredientNameResponse",
        description = "디폴트 식재료 아이디+이름 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class IngredientNameResponseDto {

    private List<IngredientItem> ingredients;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class IngredientItem {
        private Long defaultIngredientId;
        private String ingredient;
    }
}
