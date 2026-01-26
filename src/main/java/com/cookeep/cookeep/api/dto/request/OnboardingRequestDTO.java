package com.cookeep.cookeep.api.dto.request;

import java.util.List;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.onboarding.entity.CookingLevel;
import com.cookeep.cookeep.domain.onboarding.entity.FoodType;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;

public record OnboardingRequestDTO(
	// 질문 건너뛰기가 가능하므로 max=3은 서비스단에서 처리함
	List<FoodType> favoriteFoodTypes,

	CookingLevel cookingLevel,

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
