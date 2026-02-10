package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "DailyRecipeCreateResponse",
        description = "데일리 레시피 등록 결과 응답 DTO"
)
@Getter
@Builder
public class DailyRecipeCreateResponseDto {

    @Schema(description = "생성된 데일리 레시피 ID", example = "1")
    private Long dailyRecipeId;

    @Schema(description = "레시피 제목", example = "고추장 마요 달걀밥")
    private String title;

    @Schema(description = "등록 결과 메시지", example = "새로운 데일리 레시피가 등록되었습니다.")
    private String message;

    @Schema(description = "등록 시각", example = "2026-02-07T14:30:00")
    private LocalDateTime createdAt;

    public static DailyRecipeCreateResponseDto from(DailyRecipe dailyRecipe) {
        return DailyRecipeCreateResponseDto.builder()
                .dailyRecipeId(dailyRecipe.getId())
                .title(dailyRecipe.getTitle())
                .message("새로운 데일리 레시피가 등록되었습니다.")
                .createdAt(dailyRecipe.getCreatedAt())
                .build();
    }
}
