package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.user.entity.UserStatus;

public record LoginResponseDTO(
	Long userId, String accessToken,
	String refreshToken, UserStatus userStatus, boolean isRewarded // isRewarded - 14일 이상 미접속 후 접속 시 쿠키 보상, 해당 보상 지급 여부
) {
}
