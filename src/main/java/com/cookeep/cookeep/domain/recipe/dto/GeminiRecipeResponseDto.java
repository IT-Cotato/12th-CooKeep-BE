package com.cookeep.cookeep.domain.recipe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GeminiRecipeResponseDto {

    private String title;

    private Ingredients ingredients;

    private List<String> steps;

    @JsonProperty("youtube_references")
    private List<YoutubeReference> youtubeReferences;

    @Getter
    @NoArgsConstructor
    public static class Ingredients {

        @JsonProperty("user_ingredients")
        private List<UserIngredient> userIngredients;

        @JsonProperty("additional_ingredients")
        private List<ExtraIngredient> additionalIngredients;

        @JsonProperty("optional_ingredients")
        private List<ExtraIngredient> optionalIngredients;
    }

    @Getter
    @NoArgsConstructor
    public static class UserIngredient {
        private String type;
        private Long referenceId;
        private String name;
        private Integer quantity;
        private String unit;
    }

    @Getter
    @NoArgsConstructor
    public static class ExtraIngredient {
        private String name;
        private Integer quantity;
        private String unit;
    }

    @Getter
    @NoArgsConstructor
    public static class YoutubeReference {
        private String title;
        private String url;
        private String thumbnail;
    }

}
