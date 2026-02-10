package com.cookeep.cookeep.domain.recipe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(
        name = "GeminiRecipeResponse",
        description = "Gemini AI가 생성한 레시피 응답 DTO"
)
@Getter
@NoArgsConstructor
public class GeminiRecipeResponseDto {

    @Schema(
            description = "레시피 제목",
            example = "간단한 김치볶음밥",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String title;

    @Schema(
            description = "레시피 재료 정보",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Ingredients ingredients;

    @Schema(
            description = "조리 단계 목록",
            example = "[\"1. 팬에 기름을 두른다\", \"2. 재료를 볶는다\", \"3. 밥을 넣고 섞는다\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<String> steps;

    @Schema(
            description = "참고용 유튜브 영상 목록",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonProperty("youtube_search_queries")
    private List<String> youtubeSearchQueries;

    @Schema(
            name = "GeminiRecipeIngredients",
            description = "레시피 재료 구성 DTO"
    )
    @Getter
    @NoArgsConstructor
    public static class Ingredients {

        @Schema(
                description = "유저가 보유한 재료 목록",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("user_ingredients")
        private List<UserIngredient> userIngredients;

        @Schema(
                description = "추가로 필요한 재료 목록",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("additional_ingredients")
        private List<ExtraIngredient> additionalIngredients;

        @Schema(
                description = "선택 재료 목록",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("optional_ingredients")
        private List<ExtraIngredient> optionalIngredients;
    }

    @Schema(
            name = "GeminiRecipeUserIngredient",
            description = "유저 보유 재료 정보 DTO"
    )
    @Getter
    @NoArgsConstructor
    public static class UserIngredient {

        @Schema(
                description = "유저 식재료 ID (user_ingredients.ingredients_id)",
                example = "7",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("ingredientId")
        private Long ingredientId;

        @Schema(
                description = "재료 이름",
                example = "양파",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String name;

        @Schema(
                description = "수량",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1"
        )
        private Integer quantity;

        @Schema(
                description = "수량 단위",
                example = "PIECE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String unit;
    }

    @Schema(
            name = "GeminiRecipeExtraIngredient",
            description = "추가 또는 선택 재료 DTO"
    )
    @Getter
    @NoArgsConstructor
    public static class ExtraIngredient {

        @Schema(
                description = "재료 이름",
                example = "소금"
        )
        private String name;

        @Schema(
                description = "수량",
                example = "1"
        )
        private Integer quantity;

        @Schema(
                description = "수량 단위",
                example = "PIECE"
        )
        private String unit;
    }

    @Schema(
            name = "GeminiRecipeYoutubeReference",
            description = "레시피 참고 유튜브 영상 정보 DTO"
    )
    @Getter
    @NoArgsConstructor
    public static class YoutubeReference {

        @Schema(
                description = "영상 제목",
                example = "김치볶음밥 황금레시피",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String title;

        @Schema(
                description = "유튜브 영상 URL",
                example = "https://www.youtube.com/watch?v=abcdef",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String url;

        @Schema(
                description = "영상 썸네일 URL",
                example = "https://img.youtube.com/vi/abcdef/0.jpg",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        private String thumbnail;
    }

}
