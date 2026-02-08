package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Schema(
        name = "DailyRecipeCalendarResponse",
        description = "캘린더 마킹용 데일리 레시피 날짜별 응답 DTO"
)
@Getter
@Builder
public class DailyRecipeCalendarResponseDto {

    @Schema(description = "요리 기록이 있는 날짜", example = "2025-12-15")
    private LocalDate date;

    @Schema(description = "해당 날짜의 첫 번째 레시피 이미지 URL")
    private String recipeImageUrl;

    public static DailyRecipeCalendarResponseDto of(LocalDate date, String recipeImageUrl) {
        return DailyRecipeCalendarResponseDto.builder()
                .date(date)
                .recipeImageUrl(recipeImageUrl)
                .build();
    }
}
