package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "비선호 식재료 조회 응답 DTO")
@Getter
@Builder
@AllArgsConstructor
public class DislikeIngredientResponseDto {

    @Schema(description = "비선호 식재료명 목록", example = "[\"상추\", \"깻잎\"]")
    private List<String> dislikedIngredients;
}
