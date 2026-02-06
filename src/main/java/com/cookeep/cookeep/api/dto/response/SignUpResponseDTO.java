package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.user.entity.UserStatus;

public record SignUpResponseDTO(
	Long userId, String accessToken,
	String refreshToken
) {
}
