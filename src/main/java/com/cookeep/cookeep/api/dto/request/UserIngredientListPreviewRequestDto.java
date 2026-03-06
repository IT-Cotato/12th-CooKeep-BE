package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(
        name = "UserIngredientListPreviewRequest",
        description = "(2) 식재료 기본 정보 일괄 조회 요청 DTO"
)
public class UserIngredientListPreviewRequestDto {

    @Valid
    @NotEmpty(message = "식재료 목록은 1개 이상이어야 합니다.")
    @Schema(description = "조회할 식재료 목록 (type + referenceId만 필요)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UserIngredientPreviewRequestDto> ingredients;
}
