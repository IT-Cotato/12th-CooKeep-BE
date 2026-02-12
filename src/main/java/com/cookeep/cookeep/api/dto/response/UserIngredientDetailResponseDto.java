package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Schema(
        name = "UserIngredientDetailResponse",
        description = "식재료 상세 조회 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class UserIngredientDetailResponseDto {

    @Schema(
            description = "유저 식재료 ID",
            example = "1",
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
            description = "보관 장소",
            example = "FRIDGE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Storage storage;

    @Schema(
            description = "유통기한",
            example = "2026-12-25",
            type = "string",
            format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate expirationDate;

    @Schema(
            description = "수량",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer quantity;

    @Schema(
            description = "남은 일수 (D-day, 음수면 D+로 표시)",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer leftDays;

    @Schema(
            description = "재료의 단위",
            example = "PACK",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Unit unit;

    @Schema(
            description = "메모",
            example = "샐러드용",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String memo;

    @Schema(
            description = "AI 팁 (보관 팁)",
            example = "키친타월로 감싸 밀봉 보관하면 수분이 유지돼요.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String aiTip;

    @Schema(
            description = "식재료 이미지 URL",
            example = "https://s3.amazonaws.com/cookeep/ingredients/default.png",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String imageUrl;
}
