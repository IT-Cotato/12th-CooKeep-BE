package com.cookeep.cookeep.domain.recipe.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class YoutubeReferenceDto {

    private String title;
    private String url;
    private String thumbnail;
}
