package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.cookeep.cookeep.domain.verification.application.EmailVerificationService;
import com.cookeep.cookeep.security.JwtTokenProvider;
import com.cookeep.cookeep.security.TokenClaims;

@ExtendWith(MockitoExtension.class)
class AuthSessionRotationServiceTest {

	@Mock private UserRepository userRepository;
	@Mock private UserAuthRepository userAuthRepository;
	@Mock private AuthSessionStore authSessionStore;
	@Mock private JwtTokenProvider jwtTokenProvider;
	@Mock private UserReader userReader;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private LoginPasswordFailureService loginPasswordFailureService;
	@Mock private NicknameGenerator nicknameGenerator;
	@Mock private EmailVerificationService emailVerificationService;
	@Mock private CookieService cookieService;
	@Mock private WebPushSubscriptionRepository webPushSubscriptionRepository;

	private AuthService authService;
	private User user;
	private TokenClaims claims;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			userRepository,
			userAuthRepository,
			authSessionStore,
			jwtTokenProvider,
			userReader,
			passwordEncoder,
			loginPasswordFailureService,
			nicknameGenerator,
			emailVerificationService,
			cookieService,
			webPushSubscriptionRepository,
			List.of()
		);

		user = User.builder()
			.userId(1L)
			.email("user@example.com")
			.userStatus(UserStatus.ACTIVE)
			.lastAccessAt(LocalDateTime.now())
			.build();
		claims = new TokenClaims(1L, "session-id", Instant.now().plusSeconds(3600));
	}

	@Test
	void refreshRotatesTokenAndIssuesTokensForSameSession() {
		given(jwtTokenProvider.parseRefreshToken("current-refresh")).willReturn(claims);
		given(userReader.readById(1L)).willReturn(user);
		given(jwtTokenProvider.createRefreshToken(1L, "session-id", claims.expiresAt()))
			.willReturn("next-refresh");
		given(authSessionStore.rotate(
			any(), any(), any(), any()
		)).willReturn(RefreshRotationResult.ROTATED);
		given(jwtTokenProvider.createAccessToken(1L, "session-id")).willReturn("next-access");

		var result = authService.tokenRefresh("current-refresh");

		assertThat(result.response().accessToken()).isEqualTo("next-access");
		assertThat(result.refreshToken()).isEqualTo("next-refresh");
		assertThat(result.refreshExpiresInSeconds()).isBetween(3500L, 3600L);
		verify(authSessionStore).rotate(
			any(), any(), any(), any()
		);
	}

	@Test
	void reusedRefreshTokenRevokesSessionAndReturnsDedicatedError() {
		given(jwtTokenProvider.parseRefreshToken("reused-refresh")).willReturn(claims);
		given(userReader.readById(1L)).willReturn(user);
		given(jwtTokenProvider.createRefreshToken(1L, "session-id", claims.expiresAt()))
			.willReturn("next-refresh");
		given(authSessionStore.rotate(
			any(), any(), any(), any()
		)).willReturn(RefreshRotationResult.REUSE_DETECTED);

		assertThatThrownBy(() -> authService.tokenRefresh("reused-refresh"))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

		verify(jwtTokenProvider, never()).createAccessToken(any(), any());
	}

	@Test
	void refreshTokenFromOldLoginDoesNotRevokeCurrentSession() {
		given(jwtTokenProvider.parseRefreshToken("old-refresh")).willReturn(claims);
		given(userReader.readById(1L)).willReturn(user);
		given(jwtTokenProvider.createRefreshToken(1L, "session-id", claims.expiresAt()))
			.willReturn("unused-next-refresh");
		given(authSessionStore.rotate(
			any(), any(), any(), any()
		)).willReturn(RefreshRotationResult.DIFFERENT_SESSION);

		assertThatThrownBy(() -> authService.tokenRefresh("old-refresh"))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

		verify(authSessionStore, never()).revoke(any());
	}
}
