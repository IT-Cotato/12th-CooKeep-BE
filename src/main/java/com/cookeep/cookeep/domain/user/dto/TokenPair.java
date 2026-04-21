package com.cookeep.cookeep.domain.user.dto;

public record TokenPair(
	String accessToken, String refreshToken, boolean isRewarded
	// isRewarded - 14일 이상 미접속 후 접속 시 쿠키 보상, 해당 보상 지급 여부
) {
}
