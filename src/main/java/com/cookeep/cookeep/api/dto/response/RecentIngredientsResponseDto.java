package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "RecentIngredientsResponse",
        description = "최근 추가한 재료 목록 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class RecentIngredientsResponseDto {

    @Schema(description = "최근 추가한 재료 목록 (등록 순서 오름차순)")
    private List<RecentIngredientItem> ingredients;

    @Schema(
            name = "RecentIngredientItem",
            description = "최근 추가한 개별 재료 항목"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecentIngredientItem {

        @Schema(description = "유저 식재료 ID", example = "12")
        private Long ingredientId;

        @Schema(
                description = "식재료 타입",
                example = "DEFAULT",
                allowableValues = {"DEFAULT", "CUSTOM"}
        )
        private String type;

        @Schema(description = "식재료 이름", example = "사과")
        private String name;

        @Schema(
                description = "식재료 이미지 URL",
                example = "https://cookeep-images.s3.ap-northeast-2.amazonaws.com/ingredients/apple.png"
        )
        private String imageUrl;
    }
}
