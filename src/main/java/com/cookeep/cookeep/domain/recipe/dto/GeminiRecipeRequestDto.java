package com.cookeep.cookeep.domain.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Schema(
        name = "GeminiRecipeRequest",
        description = "Gemini AI 레시피 생성을 위한 요청 DTO"
)
@Getter
@AllArgsConstructor
public class GeminiRecipeRequestDto {

    private List<Content> contents;
    private GenerationConfig generationConfig;

    public static GeminiRecipeRequestDto from(String prompt) {
        return new GeminiRecipeRequestDto(
                List.of(new Content(List.of(new Part(prompt)))),
                buildGenerationConfig()
        );
    }


    // 응답 스키마 정의
    private static GenerationConfig buildGenerationConfig() {

        // unit enum (additional / optional 에만 적용)
        Map<String, Object> unitEnum = Map.of(
                "type", "string",
                "enum", List.of("개", "팩", "봉지", "병", "묶음", "캔", "g", "ml", "티스푼", "테이블스푼")
        );

        // user_ingredients
        Map<String, Object> userIngredient = Map.of(
                "type", "object",
                "properties", Map.of(
                        "ingredientId", Map.of("type", "integer"),
                        "name",         Map.of("type", "string"),
                        "quantity",     Map.of("type", "number", "minimum", 0.1),
                        "unit",         Map.of("type", "string")   // user 단위는 자유
                ),
                "required", List.of("ingredientId", "name", "quantity", "unit")
        );

        // additional_ingredients
        Map<String, Object> additionalIngredient = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name",     Map.of("type", "string"),
                        "quantity", Map.of("type", "number", "minimum", 0.1),
                        "unit",     unitEnum
                        // description 키 자체를 schema에 포함 안 함
                ),
                "required", List.of("name", "quantity", "unit")
        );

        // optional_ingredients (description 필수)
        // description 필드 타입 string으로 제한 (required)
        // 상세 포맷은 서비스에서 validateAiResponse 메서드로 관리
        Map<String, Object> optionalIngredient = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name",        Map.of("type", "string"),
                        "quantity",    Map.of("type", "number", "minimum", 0.1),
                        "unit",        unitEnum,
                        "description", Map.of("type", "string")
                ),
                "required", List.of("name", "description")
        );

        // ingredients 객체
        Map<String, Object> ingredients = Map.of(
                "type", "object",
                "properties", Map.of(
                        "user_ingredients",      Map.of("type", "array", "items", userIngredient),
                        "additional_ingredients", Map.of("type", "array", "items", additionalIngredient),
                        "optional_ingredients",   Map.of("type", "array", "items", optionalIngredient)
                ),
                "required", List.of("user_ingredients")
        );

        // 최상위 schema
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "title",       Map.of("type", "string"),
                        "ingredients", ingredients,
                        "steps",       Map.of("type", "array", "items", Map.of("type", "string")),
                        "youtube_search_queries", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        )
                ),
                "required", List.of("title", "ingredients", "steps", "youtube_search_queries")
        );

        return new GenerationConfig("application/json", schema);
    }

    // 내부 메서드
    @Schema(
            name = "GeminiRecipeRequestContent",
            description = "Gemini 요청 콘텐츠 단위 DTO"
    )
    @Getter
    @AllArgsConstructor
    static class Content {
        private List<Part> parts;
    }

    @Schema(
            name = "GeminiRecipeRequestPart",
            description = "Gemini 요청 프롬프트 파트 DTO"
    )
    @Getter
    @AllArgsConstructor
    static class Part {
        private String text;
    }

    @Getter
    @AllArgsConstructor
    static class GenerationConfig {
        private String responseMimeType;
        private Map<String, Object> responseSchema;
    }

}
