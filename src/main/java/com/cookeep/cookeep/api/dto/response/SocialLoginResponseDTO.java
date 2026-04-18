package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.user.entity.NextStep;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

// nextStep은 null일 경우 JSON 응답에서 제외함
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SocialLoginResponseDTO(
	Long userId, String accessToken,
	String refreshToken, UserStatus userStatus,
	NextStep nextStep, // nullable
	boolean isRewarded // isRewarded - 14일 이상 미접속 후 접속 시 쿠키 보상, 해당 보상 지급 여부
) {
}
