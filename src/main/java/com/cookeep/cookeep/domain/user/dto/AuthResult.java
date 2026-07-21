package com.cookeep.cookeep.domain.user.dto;

// HTTP 응답에 노출할 데이터와 쿠키로만 전달할 refresh token을 분리한 인증 처리 결과
public record AuthResult<T>(
	T response,
	String refreshToken
) {
}
