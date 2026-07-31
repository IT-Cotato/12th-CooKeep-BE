package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.ResetPasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.dto.OAuthUserInfoDTO;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserAuth;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.cookeep.cookeep.domain.verification.application.EmailVerificationService;
import com.cookeep.cookeep.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthSessionLifecycleServiceTest {

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
	@Mock private OAuthProvider oAuthProvider;

	private AuthService authService;

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
			List.of(oAuthProvider)
		);
	}

	@Test
	void loginCreatesSessionUsingTheSameSessionIdAsBothTokens() {
		User user = activeUser(1L, "login@example.com");
		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password1", user.getPassword())).willReturn(true);
		given(jwtTokenProvider.createAccessToken(eq(1L), anyString())).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(eq(1L), anyString(), any(Instant.class)))
			.willReturn("refresh-token");

		authService.login(new LoginRequestDTO(user.getEmail(), "password1"));

		ArgumentCaptor<String> accessSessionId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> refreshSessionId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> storedSessionId = ArgumentCaptor.forClass(String.class);
		verify(jwtTokenProvider).createAccessToken(eq(1L), accessSessionId.capture());
		verify(jwtTokenProvider).createRefreshToken(
			eq(1L),
			refreshSessionId.capture(),
			any(Instant.class)
		);
		verify(authSessionStore).create(
			eq(1L),
			storedSessionId.capture(),
			eq("refresh-token"),
			eq(AuthSessionPolicy.REFRESH_TOKEN_TTL)
		);

		assertThat(accessSessionId.getValue())
			.isEqualTo(refreshSessionId.getValue())
			.isEqualTo(storedSessionId.getValue());
	}

	@Test
	void signupCreatesRedisSession() {
		User user = activeUser(2L, "signup@example.com");
		SignupRequestDTO request =
			new SignupRequestDTO(user.getEmail(), "password1", "password1", true);
		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.empty());
		given(passwordEncoder.encode("password1")).willReturn("encoded-password");
		given(nicknameGenerator.generateRandomNickname()).willReturn("nickname");
		given(userRepository.saveAndFlush(any(User.class))).willReturn(user);
		given(jwtTokenProvider.createAccessToken(eq(2L), anyString())).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(eq(2L), anyString(), any(Instant.class)))
			.willReturn("refresh-token");
		given(userAuthRepository.save(any(UserAuth.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		authService.signUp(request);

		verify(authSessionStore).create(
			eq(2L),
			anyString(),
			eq("refresh-token"),
			eq(AuthSessionPolicy.REFRESH_TOKEN_TTL)
		);
	}

	@Test
	void socialLoginCreatesRedisSession() {
		User user = activeUser(3L, "social@example.com");
		UserAuth userAuth = UserAuth.builder()
			.user(user)
			.provider(Provider.KAKAO)
			.providerUserId("social-id")
			.build();
		given(oAuthProvider.provider()).willReturn(Provider.KAKAO);
		given(oAuthProvider.getAccessToken("code", null)).willReturn("social-access-token");
		given(oAuthProvider.getUserInfo("social-access-token"))
			.willReturn(new OAuthUserInfoDTO("social-id", user.getEmail()));
		given(userAuthRepository.findByProviderAndProviderUserId(Provider.KAKAO, "social-id"))
			.willReturn(Optional.of(userAuth));
		given(jwtTokenProvider.createAccessToken(eq(3L), anyString())).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(eq(3L), anyString(), any(Instant.class)))
			.willReturn("refresh-token");

		authService.socialLogin(Provider.KAKAO, "code", null);

		verify(authSessionStore).create(
			eq(3L),
			anyString(),
			eq("refresh-token"),
			eq(AuthSessionPolicy.REFRESH_TOKEN_TTL)
		);
	}

	@Test
	void resetLogoutAndWithdrawRevokeRedisSessions() {
		User resetUser = activeUser(4L, "reset@example.com");
		User withdrawnUser = activeUser(6L, "withdraw@example.com");
		ResetPasswordRequestDTO request =
			new ResetPasswordRequestDTO(resetUser.getEmail(), "new-password1", "new-password1");
		given(userRepository.findByEmail(resetUser.getEmail())).willReturn(Optional.of(resetUser));
		given(passwordEncoder.matches("new-password1", resetUser.getPassword())).willReturn(false);
		given(passwordEncoder.encode("new-password1")).willReturn("new-encoded-password");
		given(userReader.readById(6L)).willReturn(withdrawnUser);

		authService.resetPassword(request);
		authService.logout(5L);
		authService.withdraw(6L);

		verify(authSessionStore).revoke(4L);
		verify(authSessionStore).revoke(5L);
		verify(authSessionStore).revoke(6L);
	}

	private User activeUser(Long userId, String email) {
		return User.builder()
			.userId(userId)
			.email(email)
			.password("encoded-password")
			.nickname("nickname")
			.userStatus(UserStatus.ACTIVE)
			.lastAccessAt(LocalDateTime.now())
			.build();
	}
}
