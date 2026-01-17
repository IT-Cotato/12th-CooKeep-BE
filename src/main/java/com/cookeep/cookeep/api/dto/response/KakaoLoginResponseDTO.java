package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.user.entity.NextStep;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

// nextStep은 null일 경우 JSON 응답에서 제외함
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoLoginResponseDTO(
	Long userId, String accessToken,
	String refreshToken, UserStatus userStatus,
	NextStep nextStep // nullable
) {
}
