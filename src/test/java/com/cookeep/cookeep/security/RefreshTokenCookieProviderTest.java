package com.cookeep.cookeep.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieProviderTest {

	@Test
	void create_buildsSecureHttpOnlyRefreshTokenCookie() {
		RefreshTokenCookieProvider provider = new RefreshTokenCookieProvider(true);

		ResponseCookie cookie = provider.create("refresh-token", Duration.ofDays(14));

		assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieProvider.COOKIE_NAME);
		assertThat(cookie.getValue()).isEqualTo("refresh-token");
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getPath()).isEqualTo("/api/auth/refresh");
		assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(14));
		assertThat(cookie.getDomain()).isNull();
	}

	@Test
	void create_canDisableSecureForLocalHttpTools() {
		RefreshTokenCookieProvider provider = new RefreshTokenCookieProvider(false);

		ResponseCookie cookie = provider.create("refresh-token", Duration.ofDays(14));

		assertThat(cookie.isSecure()).isFalse();
		assertThat(cookie.isHttpOnly()).isTrue();
	}

	@Test
	void delete_expiresCookieWithSameScope() {
		RefreshTokenCookieProvider provider = new RefreshTokenCookieProvider(true);

		ResponseCookie cookie = provider.delete();

		assertThat(cookie.getName()).isEqualTo(RefreshTokenCookieProvider.COOKIE_NAME);
		assertThat(cookie.getValue()).isEmpty();
		assertThat(cookie.getPath()).isEqualTo("/api/auth/refresh");
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
		assertThat(cookie.getDomain()).isNull();
	}
}
