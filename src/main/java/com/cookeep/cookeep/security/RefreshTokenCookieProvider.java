package com.cookeep.cookeep.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProvider {

	public static final String COOKIE_NAME = "refreshToken";
	private static final String COOKIE_PATH = "/api/auth/refresh";
	private static final Duration COOKIE_MAX_AGE = Duration.ofDays(14);

	private final boolean secure;

	public RefreshTokenCookieProvider(
		@Value("${REFRESH_COOKIE_SECURE:true}") boolean secure
	) {
		this.secure = secure;
	}

	public ResponseCookie create(String refreshToken) {
		return baseCookie(refreshToken)
			// HttpOnly는 브라우저 JavaScript가 refresh token을 읽지 못하게 해 XSS 탈취 위험을 낮춘다.
			.httpOnly(true)
			// 운영에서는 HTTPS로만 전송한다. HTTP 기반 로컬 도구가 필요할 때만 환경변수로 비활성화한다.
			.secure(secure)
			// 프론트와 API가 같은 site(cookeep.kr)이므로 cross-site 요청에는 쿠키를 보내지 않는다.
			.sameSite("Lax")
			// refresh token이 인증 영역의 다른 API에 불필요하게 첨부되지 않도록 갱신 경로로 제한한다.
			.path(COOKIE_PATH)
			.maxAge(COOKIE_MAX_AGE)
			.build();
	}

	public ResponseCookie delete() {
		return baseCookie("")
			.httpOnly(true)
			.secure(secure)
			.sameSite("Lax")
			// 브라우저가 기존 쿠키를 찾아 삭제하려면 발급할 때와 이름 및 Path가 같아야 한다.
			.path(COOKIE_PATH)
			.maxAge(Duration.ZERO)
			.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(COOKIE_NAME, value);
	}
}
