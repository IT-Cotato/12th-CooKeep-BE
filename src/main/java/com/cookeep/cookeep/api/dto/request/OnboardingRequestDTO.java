package com.cookeep.cookeep.api.dto.request;

import java.util.List;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;

public record OnboardingRequestDTO(
	List<String> dislikedIngredients,

	GoalActionType goalActionType,

	Integer targetCount
) {
	public OnboardingRequestDTO {
		if (goalActionType != null) {
			if (targetCount == null) { // goalActionType이 존재하는데, targetCount을 입력하지 않았다면 에러 발생
				throw new AppException(ErrorCode.INVALID_WEEKLY_GOAL_TARGET_COUNT);
			} else if (targetCount < 1 || targetCount > 10) { // 1~10 사이가 아닐 경우 에러 발생
				throw new AppException(ErrorCode.INVALID_TARGET_COUNT);
			}
		}
	}
}
