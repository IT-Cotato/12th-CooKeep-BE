package com.cookeep.cookeep.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProvider {

	public static final String COOKIE_NAME = "refreshToken";
	private static final String COOKIE_PATH = "/api/auth/refresh";

	private final boolean secure;

	public RefreshTokenCookieProvider(
		@Value("${REFRESH_COOKIE_SECURE:true}") boolean secure
	) {
		this.secure = secure;
	}

	public ResponseCookie create(String refreshToken, Duration maxAge) {
		return baseCookie(refreshToken)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Lax")
			.path(COOKIE_PATH)
			.maxAge(maxAge)
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
