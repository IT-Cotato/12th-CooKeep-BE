package com.cookeep.cookeep.domain.recipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true) //AI 에러 방지
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
        private Double quantity;

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

        @Schema(
                description = """
                    재료 설명 (선택 재료일 경우 필수).
                    - additional_ingredients는 반드시 null이어야 함.
                    - optional_ingredients는 반드시 아래 형식 중 하나로 작성:
                      1) \"이 재료는 [다른 재료]로 대체 가능합니다\"
                      2) \"이 재료는 생략 가능합니다\"
                    """,
                example = "이 재료는 쪽파로 대체 가능합니다"
        )
        private String description;
    }

}
