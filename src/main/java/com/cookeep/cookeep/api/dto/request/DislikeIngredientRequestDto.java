package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "비선호 식재료 수정 요청 DTO")
public record DislikeIngredientRequestDto(
        @NotNull
        @Schema(description = "수정할 비선호 식재료명 전체 목록 (빈 배열이면 전체 삭제)", example = "[\"상추\", \"깻잎\"]")
        List<String> dislikedIngredients
) { }
