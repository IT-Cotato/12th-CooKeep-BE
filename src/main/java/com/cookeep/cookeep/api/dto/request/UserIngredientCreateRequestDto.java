package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(
        name = "UserIngredientCreateRequest",
        description = "유저 식재료 등록 요청 DTO"
)
public class UserIngredientCreateRequestDto {

    @NotNull(message = "식재료 타입은 필수입니다.")
    @Schema(
            description = "식재료 타입 (기본/커스텀 등 시스템이 정의한 타입)",
            example = "CUSTOM",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Type type;

    @NotNull(message = "식재료 ID는 필수입니다.")
    @Schema(
            description = "참조 ID (type에 따라 참조 대상이 달라짐). 예: CUSTOM이면 CustomIngredient의 ID, DEFAULT면 기본 식재료 ID",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long referenceId;

    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "수량은 양수여야 합니다.")
    @Schema(
            description = "수량 (양수)",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "1"
    )
    private Integer quantity;

    @NotNull(message = "단위는 필수입니다.")
    @Schema(
            description = "수량 단위",
            example = "EA",
            allowableValues = {"PIECE", "PACK", "BAG", "BOTTLE", "BUNDLE", "CAN", "GRAM", "MILLILITER"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Unit unit;

    @Schema(
            description = "보관 장소 (미입력 시 서버 정책/기본값 적용 가능)",
            example = "FRIDGE",
            allowableValues = {"FRIDGE", "FREEZER", "PANTRY"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Storage storage;

    @Schema(
            description = "소비/만료 날짜 (yyyy-MM-dd). 미입력 시 서버에서 기본값 계산 가능",
            example = "2026-01-20",
            type = "string",
            format = "date",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDate expirationDate;

    @Schema(
            description = "메모(선택)",
            example = "유통기한 짧음. 먼저 사용",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String memo;
}
