package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(
        name = "PaginatedIngredientsResponse",
        description = "페이지네이션된 식재료 목록 응답 DTO (장소별 전체보기 화면 내 스크롤에 이용)"
)
@Getter
@Builder
@AllArgsConstructor
public class PaginatedIngredientsResponseDto {
    @Schema(
            description = "식재료 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<RefrigeratorIngredientsResponseDto.IngredientItem> content;

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
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean hasNext;
}
