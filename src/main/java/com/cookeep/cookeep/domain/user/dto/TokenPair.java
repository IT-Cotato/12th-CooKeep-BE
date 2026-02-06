package com.cookeep.cookeep.domain.user.dto;

public record TokenPair(
	String accessToken, String refreshToken
) {
}
