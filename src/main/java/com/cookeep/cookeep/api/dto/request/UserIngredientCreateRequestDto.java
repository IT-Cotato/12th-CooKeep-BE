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
        description = "유저 식재료 등록 요청 DTO (단일항목)"
)
public class UserIngredientCreateRequestDto {

    @NotNull(message = "식재료 타입은 필수입니다.")
    @Schema(
            description = "식재료 타입 (DEFAULT / CUSTOM)",
            example = "CUSTOM",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Type type;

    @NotNull(message = "식재료 ID는 필수입니다.")
    @Schema(
            description = "참조 ID (DefaultIngredient/CustomIngredient DB의 ID)",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long referenceId;

    @Positive(message = "수량은 양수여야 합니다.")
    @Schema(description = "수량 (미입력 시 기본값 1)", example = "2", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer quantity;

    @Schema(
            description = "수량 단위 (미입력 시 DEFAULT=db값, CUSTOM=PIECE)",
            example = "PIECE",
            allowableValues = {"PIECE", "PACK", "BAG", "BOTTLE", "BUNDLE", "CAN", "GRAM", "MILLILITER"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Unit unit;

    @Schema(
            description = "보관 장소 (미입력 시 db 기본값)",
            example = "FRIDGE",
            allowableValues = {"FRIDGE", "FREEZER", "PANTRY"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Storage storage;

    @Schema(
            description = "소비/만료 날짜 (yyyy-MM-dd). 미입력 시 db expirationDays 기준 계산",
            example = "2026-07-20",
            type = "string",
            format = "date",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDate expirationDate;

    @Schema(
            description = "메모(선택). 미입력 시 null",
            example = "유통기한 짧음. 먼저 사용",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String memo;
}
