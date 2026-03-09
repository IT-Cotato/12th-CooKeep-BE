package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(name = "UserIngredientListCreateRequest", description = "유저 식재료 일괄 등록 요청 DTO")
public class UserIngredientListCreateRequestDto {

    @Valid
    @NotEmpty(message = "식재료 목록은 1개 이상이어야 합니다.")
    @Schema(description = "등록할 식재료 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UserIngredientCreateRequestDto> ingredients;

}
