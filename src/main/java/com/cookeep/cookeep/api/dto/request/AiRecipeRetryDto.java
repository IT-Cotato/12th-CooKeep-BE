package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "AiRecipeRequest",
        description = "AI 레시피 생성 또는 재요청 요청 DTO"
)
@Getter
@NoArgsConstructor
public class AiRecipeRetryDto {

    @NotNull(message = "세션 아이디는 필수 입력 값입니다.")
    private Long sessionId;
}
