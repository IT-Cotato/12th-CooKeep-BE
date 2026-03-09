package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(
        name = "UserIngredientPreviewRequest",
        description = "식재료 기본 정보 조회 요청 DTO (단일 항목)"
)
public class UserIngredientPreviewRequestDto {

    @NotNull(message = "식재료 타입은 필수입니다.")
    @Schema(
            description = "식재료 타입 (DEFAULT / CUSTOM)",
            example = "DEFAULT",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Type type;

    @NotNull(message = "식재료 ID는 필수입니다.")
    @Schema(
            description = "참조 ID (DefaultIngredient 또는 CustomIngredient의 DB ID)",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long referenceId;
}
