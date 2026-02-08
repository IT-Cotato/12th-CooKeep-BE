package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "DailyRecipeVisibilityRequest",
        description = "데일리 레시피 공개 범위 수정 요청 DTO"
)
@Getter
@NoArgsConstructor
public class DailyRecipeVisibilityRequestDto {

    @Schema(
            description = "공개 여부 (true: 쿠킵스에 공개, false: 나만 보기)",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "공개 여부는 필수입니다.")
    private Boolean isPublic;
}
