package com.cookeep.cookeep.api.dto.request;

import java.util.List;

import com.cookeep.cookeep.domain.onboarding.entity.CookingLevel;
import com.cookeep.cookeep.domain.onboarding.entity.FoodType;

public record OnboardingRequestDTO(
	// 질문 건너뛰기가 가능하므로 max=3은 서비스단에서 처리함
	List<FoodType> favoriteFoodTypes,

	CookingLevel cookingLevel,

	String weeklyGoal
) {
}
