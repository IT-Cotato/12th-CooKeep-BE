package com.cookeep.cookeep.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtSessionTokenTest {

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(
			"access-secret-key-for-test-must-be-at-least-32-bytes",
			"refresh-secret-key-for-test-must-be-at-least-32-bytes"
		);
	}

	@Test
	void accessAndRefreshTokensCarrySameSessionId() {
		Instant refreshExpiresAt = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);

		String accessToken = jwtTokenProvider.createAccessToken(1L, "session-id");
		String refreshToken = jwtTokenProvider.createRefreshToken(
			1L,
			"session-id",
			refreshExpiresAt
		);

		TokenClaims accessClaims = jwtTokenProvider.parseAccessToken(accessToken);
		TokenClaims refreshClaims = jwtTokenProvider.parseRefreshToken(refreshToken);

		assertThat(accessClaims.userId()).isEqualTo(1L);
		assertThat(accessClaims.sessionId()).isEqualTo("session-id");
		assertThat(refreshClaims.userId()).isEqualTo(1L);
		assertThat(refreshClaims.sessionId()).isEqualTo("session-id");
		assertThat(refreshClaims.expiresAt()).isEqualTo(refreshExpiresAt);
	}

	@Test
	void legacyTokenWithoutSessionIdIsRejected() {
		String legacyAccessToken = jwtTokenProvider.createAccessToken(1L);
		String legacyRefreshToken = jwtTokenProvider.createRefreshToken(1L);

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(legacyAccessToken))
			.isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> jwtTokenProvider.parseRefreshToken(legacyRefreshToken))
			.isInstanceOf(JwtException.class);
	}
}
