package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import jakarta.validation.constraints.NotNull;

public record WeeklyGoalRequestDto(
        @NotNull(message = "목표 타입은 필수입니다.")
        GoalActionType goalActionType,

        @NotNull(message = "목표 횟수는 필수입니다.")
        Integer targetCount
) {
    public WeeklyGoalRequestDto {
        if (targetCount != null && (targetCount < 1 || targetCount > 10)) {
            throw new AppException(ErrorCode.INVALID_TARGET_COUNT);
        }
    }
}
