package com.cookeep.cookeep.domain.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "YoutubeReference",
        description = "레시피 참고용 유튜브 영상 정보 DTO"
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeReferenceDto {

    @Schema(
            description = "유튜브 영상 제목 (검색 결과가 없는 경우 안내 문구가 들어감)",
            example = "김치볶음밥 황금레시피",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String title;

    @Schema(
            description = "유튜브 영상 URL (검색 결과가 없는 경우 null)",
            example = "https://www.youtube.com/watch?v=abcdef",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String url;

    @Schema(
            description = "유튜브 영상 썸네일 URL (검색 결과가 없는 경우 null)",
            example = "https://img.youtube.com/vi/abcdef/0.jpg",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String thumbnail;

    @Schema(
            description = "실제 검색에 사용된 원본 검색어 (검색 결과 없어도 검색어는 존재함)",
            example = "김치볶음밥 만들기",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String searchQuery;
}
