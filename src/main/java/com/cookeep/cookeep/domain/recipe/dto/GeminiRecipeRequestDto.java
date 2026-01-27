package com.cookeep.cookeep.domain.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "GeminiRecipeRequest",
        description = "Gemini AI 레시피 생성을 위한 요청 DTO"
)
@Getter
@AllArgsConstructor
public class GeminiRecipeRequestDto {

    @Schema(
            description = "Gemini API에 전달되는 콘텐츠 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<Content> contents;

    public static GeminiRecipeRequestDto from(String prompt) {
        return new GeminiRecipeRequestDto(
                List.of(
                        new Content(
                                List.of(new Part(prompt))
                        )
                )
        );
    }

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
}
