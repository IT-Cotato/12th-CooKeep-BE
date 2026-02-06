package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "RefrigeratorSearchResponseDto",
        description = "냉장고 내 식재료 검색 결과 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class RefrigeratorSearchResponseDto {

    @Schema(
            description = "검색 결과 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<SearchResultItem> content;

    @Schema(
            description = "현재 페이지 번호",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int page;

    @Schema(
            description = "페이지 크기",
            example = "20",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int size;

    @Schema(
            description = "다음 페이지 존재 여부",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean hasNext;

    @Schema(
            name = "SearchResultItem",
            description = "검색 결과 항목"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SearchResultItem {

        @Schema(
                description = "유저 식재료 ID",
                example = "12",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Long ingredientId;

        @Schema(
                description = "식재료 이름",
                example = "우유",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String name;

        @Schema(
                description = "식재료 이미지 URL",
                example = "https://s3.amazonaws.com/cookeep/ingredients/default.png",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String imageUrl;

        @Schema(
                description = "보관 장소",
                example = "FRIDGE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String storage;

        @Schema(
                description = "유통기한",
                example = "2025-02-15",
                type = "string",
                format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private LocalDate expirationDate;

        @Schema(
                description = "수량",
                example = "2",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Integer quantity;

        @Schema(
                description = "단위",
                example = "PACK",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String unit;
    }
}
