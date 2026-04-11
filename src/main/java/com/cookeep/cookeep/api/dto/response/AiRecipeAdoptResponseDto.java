package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(
            description = "첫 레시피 채택 쿠키 지급 여부",
            example = "true"
    )
    private boolean recipeRewardGranted;

    @Schema(
            description = "임박 재료 사용 쿠키 지급 여부 (BONUS_URGENT_INGREDIENT_USE)",
            example = "true"
    )
    private boolean urgentIngredientRewardGranted;

    @Schema(description = "이번 채택으로 지급된 리워드 정보")
    private RewardInfo reward;

    @Getter
    @Builder
    public static class RewardInfo {
        @Schema(description = "리워드 지급 여부", example = "true")
        private Boolean granted;

        @Schema(description = "지급된 총 쿠키 포인트", example = "4")
        private Integer points;

        @Schema(description = "지급된 쿠키 로그 타입 목록",
                example = "[\"ONBOARDING_RECIPE\", \"BONUS_WEEKLY_GOAL_ACHIEVE\", \"BONUS_URGENT_INGREDIENT_USE\"]")
        private List<CookieLog.CookieLogType> grantedTypes;
    }

}
