package com.cookeep.cookeep.api.dto.response;

public record TokenRefreshResponseDTO(
	String accessToken,
	boolean isRewarded
) {
}
