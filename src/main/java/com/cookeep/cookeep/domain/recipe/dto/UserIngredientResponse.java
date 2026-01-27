package com.cookeep.cookeep.domain.recipe.dto;

import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.common.Unit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(
        name = "UserIngredientResponse",
        description = "유저가 보유한 식재료 정보 응답 DTO"
)
@Builder
@Getter
public class UserIngredientResponse {

    @Schema(
            description = "식재료 이름",
            example = "양파",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(
            description = "보유 수량",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "1"
    )
    private int quantity;

    @Schema(
            description = "수량 단위",
            example = "PIECE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Unit unit;

    @Schema(
            description = "식재료 타입",
            example = "CUSTOM",
            allowableValues = {"DEFAULT", "CUSTOM"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Type type;

    @Schema(
            description = "식재료 참조 ID (type에 따라 참조 대상이 달라짐)",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long referenceId;
}
