package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(
        name = "AiRecipeAdoptResponse",
        description = "AI 레시피 채택 결과 응답 DTO"
)
@Getter
@Builder
public class AiRecipeAdoptResponseDto {

    @Schema(
            description = "AI 레시피 세션 ID",
            example = "12",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long sessionId;

    @Schema(
            description = "채택되어 저장된 레시피 ID",
            example = "45",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long recipeId;

    @Schema(
            description = "레시피 채택 결과 메시지",
            example = "레시피가 성공적으로 채택되었습니다.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;

    @Schema(
            description = "레시피 채택 완료 시각",
            example = "2026-01-27T14:30:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime completedAt;

    @Schema(
            description = "이번 채택으로 주간 목표 달성 여부 (COOKING / USE_EXPIRING_INGREDIENT)",
            example = "false"
    )
    private boolean weeklyGoalAchieved;
}
