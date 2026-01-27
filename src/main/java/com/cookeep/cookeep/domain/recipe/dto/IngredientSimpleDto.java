package com.cookeep.cookeep.domain.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "IngredientSimple",
        description = "AI 레시피 생성을 위해 전달되는 최소 단위 재료 DTO"
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientSimpleDto {

    @Schema(
            description = "재료 타입 (DEFAULT 또는 CUSTOM)",
            example = "CUSTOM",
            allowableValues = {"DEFAULT", "CUSTOM"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "재료 타입은 필수입니다.")
    private String type;

    @Schema(
            description = "재료 참조 ID (type에 따라 참조 대상이 달라짐)",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "재료 참조 ID는 필수입니다.")
    private Long referenceId;
}
