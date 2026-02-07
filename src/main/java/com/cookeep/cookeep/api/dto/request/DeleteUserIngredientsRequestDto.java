package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(
        name = "DeleteUserIngredientsRequest",
        description = "유저 식재료 삭제 요청 DTO"
)
@Getter
@NoArgsConstructor
public class DeleteUserIngredientsRequestDto {

    @Schema(
            description = "삭제할 재료 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<Long> userIngredientIds;
}
