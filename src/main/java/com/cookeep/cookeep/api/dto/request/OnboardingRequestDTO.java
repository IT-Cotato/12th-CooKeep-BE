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
	}
}
