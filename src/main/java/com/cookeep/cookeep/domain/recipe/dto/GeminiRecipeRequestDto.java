package com.cookeep.cookeep.domain.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GeminiRecipeRequestDto {

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

    @Getter
    @AllArgsConstructor
    static class Content {
        private List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    static class Part {
        private String text;
    }
}
