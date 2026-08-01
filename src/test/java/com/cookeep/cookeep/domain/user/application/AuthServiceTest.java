package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.SendCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyCodeRequestDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
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
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;
import com.cookeep.cookeep.security.JwtTokenProvider;
import com.cookeep.cookeep.security.TokenClaims;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserAuthRepository userAuthRepository;

	@Mock
	private AuthSessionStore authSessionStore;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private UserReader userReader;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private LoginPasswordFailureService loginPasswordFailureService;

	@Mock
	private NicknameGenerator nicknameGenerator;

	@Mock
	private EmailVerificationService emailVerificationService;

	@Mock
	private CookieService cookieService;

	@Mock
	private WebPushSubscriptionRepository webPushSubscriptionRepository;

	@Mock
	private OAuthProvider oAuthProvider;

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
	@DisplayName("signUp creates user when SIGNUP email verification is completed")
	void signUp_verifiedSignupEmail_createsUser() {
		String email = "test@example.com";
		SignupRequestDTO request = new SignupRequestDTO(email, "password1", "password1", true);
		User savedUser = User.builder()
			.userId(1L)
			.email(email)
			.password("encoded-password")
			.nickname("nickname")
			.marketingConsent(true)
			.lastAccessAt(LocalDateTime.now())
			.build();

		given(userRepository.findByEmail(email)).willReturn(Optional.empty());
		given(passwordEncoder.encode("password1")).willReturn("encoded-password");
		given(nicknameGenerator.generateRandomNickname()).willReturn("nickname");
		given(userRepository.saveAndFlush(any(User.class))).willReturn(savedUser);
		given(jwtTokenProvider.createAccessToken(any(Long.class), any(String.class))).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(any(Long.class), any(String.class), any(Instant.class))).willReturn("refresh-token");
		given(userAuthRepository.save(any(UserAuth.class))).willAnswer(invocation -> invocation.getArgument(0));

		var authResult = authService.signUp(request);
		SignUpResponseDTO response = authResult.response();

		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(authResult.refreshToken()).isEqualTo("refresh-token");
		verify(emailVerificationService).assertVerified(email, VerificationPurpose.SIGNUP);
		verify(userRepository).saveAndFlush(any(User.class));
		verify(authSessionStore).create(any(), any(), any(), any());
	}

	@Test
	@DisplayName("signUp does not create user when SIGNUP email is not verified")
	void signUp_unverifiedSignupEmail_doesNotCreateUser() {
		String email = "test@example.com";
		SignupRequestDTO request = new SignupRequestDTO(email, "password1", "password1", false);
		doThrow(new AppException(ErrorCode.VERIFICATION_NOT_VERIFIED))
			.when(emailVerificationService)
			.assertVerified(email, VerificationPurpose.SIGNUP);

		assertThatThrownBy(() -> authService.signUp(request))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.VERIFICATION_NOT_VERIFIED);

		verify(userRepository, never()).saveAndFlush(any(User.class));
		verify(passwordEncoder, never()).encode(any());
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	@Test
	@DisplayName("signUp does not create user when SIGNUP email verification is expired")
	void signUp_expiredSignupEmail_doesNotCreateUser() {
		String email = "test@example.com";
		SignupRequestDTO request = new SignupRequestDTO(email, "password1", "password1", false);
		doThrow(new AppException(ErrorCode.VERIFICATION_CODE_EXPIRED))
			.when(emailVerificationService)
			.assertVerified(email, VerificationPurpose.SIGNUP);

		assertThatThrownBy(() -> authService.signUp(request))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.VERIFICATION_CODE_EXPIRED);

		verify(userRepository, never()).saveAndFlush(any(User.class));
		verify(passwordEncoder, never()).encode(any());
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	@Test
	@DisplayName("signUp does not create user when SIGNUP email verification is missing")
	void signUp_missingSignupVerification_doesNotCreateUser() {
		String email = "test@example.com";
		SignupRequestDTO request = new SignupRequestDTO(email, "password1", "password1", false);
		doThrow(new AppException(ErrorCode.VERIFICATION_NOT_FOUND))
			.when(emailVerificationService)
			.assertVerified(email, VerificationPurpose.SIGNUP);

		assertThatThrownBy(() -> authService.signUp(request))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.VERIFICATION_NOT_FOUND);

		verify(userRepository, never()).saveAndFlush(any(User.class));
		verify(passwordEncoder, never()).encode(any());
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	@Test
	@DisplayName("login increments password count when password does not match")
	void login_passwordMismatch_incrementsPasswordCount() {
		User user = user("test@example.com", UserStatus.ACTIVE);

		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
		given(passwordEncoder.matches("wrong-password", user.getPassword())).willReturn(false);
		given(loginPasswordFailureService.increasePasswordFailCount(user.getUserId())).willReturn(3);

		assertThatThrownBy(() -> authService.login(new LoginRequestDTO(user.getEmail(), "wrong-password")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.AUTH_PASSWORD_MISMATCH);

		verify(loginPasswordFailureService).increasePasswordFailCount(user.getUserId());
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	@Test
	@DisplayName("login locks user when password mismatch reaches max attempts")
	void login_maxPasswordMismatch_locksUser() {
		User user = user("test@example.com", UserStatus.ACTIVE);

		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
		given(passwordEncoder.matches("wrong-password", user.getPassword())).willReturn(false);
		given(loginPasswordFailureService.increasePasswordFailCount(user.getUserId())).willReturn(5);

		assertThatThrownBy(() -> authService.login(new LoginRequestDTO(user.getEmail(), "wrong-password")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PASSWORD_VERIFICATION_LOCKED);

		verify(loginPasswordFailureService).increasePasswordFailCount(user.getUserId());
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	@Test
	@DisplayName("login resets password count and issues tokens when password matches")
	void login_passwordMatches_resetsPasswordCountAndIssuesTokens() {
		User user = user("test@example.com", UserStatus.ACTIVE);
		user.updatePasswordCnt(3);

		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
		given(passwordEncoder.matches("password1", user.getPassword())).willReturn(true);
		given(jwtTokenProvider.createAccessToken(any(Long.class), any(String.class))).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(any(Long.class), any(String.class), any(Instant.class))).willReturn("refresh-token");

		var authResult = authService.login(new LoginRequestDTO(user.getEmail(), "password1"));
		var response = authResult.response();

		assertThat(user.getPasswordCnt()).isZero();
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(authResult.refreshToken()).isEqualTo("refresh-token");
		verify(loginPasswordFailureService, never()).increasePasswordFailCount(any());
		verify(authSessionStore).create(any(), any(), any(), any());
	}

	@Test
	@DisplayName("login does not issue tokens for locked user")
	void login_lockedUser_doesNotIssueToken() {
		assertLoginBlocked(UserStatus.LOCK, ErrorCode.USER_ACCOUNT_LOCKED);
	}

	@Test
	@DisplayName("login does not issue tokens for withdrawn user")
	void login_withdrawnUser_doesNotIssueToken() {
		assertLoginBlocked(UserStatus.WITHDRAWN, ErrorCode.USER_ACCOUNT_WITHDRAWN);
	}

	@Test
	@DisplayName("sendAccountUnlockCode sends PASSWORD_VERIFICATION code for registered user")
	void sendAccountUnlockCode_lockedUser_sendsCode() {
		User user = user("test@example.com", UserStatus.LOCK);

		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

		authService.sendAccountUnlockCode(new SendCodeRequestDTO(user.getEmail()));

		verify(emailVerificationService).sendCode(user.getEmail(), VerificationPurpose.PASSWORD_VERIFICATION);
	}

	@Test
	@DisplayName("sendAccountUnlockCode blocks withdrawn user")
	void sendAccountUnlockCode_withdrawnUser_throwsException() {
		User user = user("test@example.com", UserStatus.WITHDRAWN);

		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.sendAccountUnlockCode(new SendCodeRequestDTO(user.getEmail())))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.USER_ACCOUNT_WITHDRAWN);

		verify(emailVerificationService, never()).sendCode(any(), any());
	}

	@Test
	@DisplayName("verifyAccountUnlockCode resets password count and activates user")
	void verifyAccountUnlockCode_verifiedCode_activatesUser() {
		User user = user("test@example.com", UserStatus.LOCK);
		user.updatePasswordCnt(5);

		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

		authService.verifyAccountUnlockCode(new VerifyCodeRequestDTO(user.getEmail(), "123456"));

		verify(emailVerificationService)
			.verifyCode(user.getEmail(), VerificationPurpose.PASSWORD_VERIFICATION, "123456");
		assertThat(user.getPasswordCnt()).isZero();
		assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("verifyAccountUnlockCode fails when email is not registered")
	void verifyAccountUnlockCode_missingEmail_throwsException() {
		String email = "missing@example.com";

		given(userRepository.findByEmail(email)).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.verifyAccountUnlockCode(new VerifyCodeRequestDTO(email, "123456")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.EMAIL_NOT_REGISTERED);

		verify(emailVerificationService, never()).verifyCode(any(), any(), any());
	}

	@Test
	@DisplayName("socialLogin does not issue tokens for existing locked user auth")
	void socialLogin_existingLockedUser_doesNotIssueToken() {
		assertSocialLoginBlocked(UserStatus.LOCK, ErrorCode.USER_ACCOUNT_LOCKED);
	}

	@Test
	@DisplayName("socialLogin does not issue tokens for existing withdrawn user auth")
	void socialLogin_existingWithdrawnUser_doesNotIssueToken() {
		assertSocialLoginBlocked(UserStatus.WITHDRAWN, ErrorCode.USER_ACCOUNT_WITHDRAWN);
	}

	@Test
	@DisplayName("socialLogin does not save user auth when same email user is locked")
	void socialLogin_lockedEmailUser_doesNotSaveUserAuth() {
		assertSocialEmailUserBlocked(UserStatus.LOCK, ErrorCode.USER_ACCOUNT_LOCKED);
	}

	@Test
	@DisplayName("socialLogin does not save user auth when same email user is withdrawn")
	void socialLogin_withdrawnEmailUser_doesNotSaveUserAuth() {
		assertSocialEmailUserBlocked(UserStatus.WITHDRAWN, ErrorCode.USER_ACCOUNT_WITHDRAWN);
	}

	@Test
	@DisplayName("tokenRefresh does not issue access token for locked user")
	void tokenRefresh_lockedUser_doesNotIssueAccessToken() {
		assertTokenRefreshBlocked(UserStatus.LOCK, ErrorCode.USER_ACCOUNT_LOCKED);
	}

	@Test
	@DisplayName("tokenRefresh does not issue access token for withdrawn user")
	void tokenRefresh_withdrawnUser_doesNotIssueAccessToken() {
		assertTokenRefreshBlocked(UserStatus.WITHDRAWN, ErrorCode.USER_ACCOUNT_WITHDRAWN);
	}

	@Test
	@DisplayName("tokenRefresh rejects a missing or blank refresh token")
	void tokenRefresh_missingOrBlankToken_throwsInvalidRefreshToken() {
		assertThatThrownBy(() -> authService.tokenRefresh(null))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

		assertThatThrownBy(() -> authService.tokenRefresh(" "))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}


	private void assertLoginBlocked(UserStatus userStatus, ErrorCode errorCode) {
		User user = user("test@example.com", userStatus);
		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequestDTO(user.getEmail(), "password1")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(errorCode);

		verify(passwordEncoder, never()).matches(any(), any());
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	private void assertSocialLoginBlocked(UserStatus userStatus, ErrorCode errorCode) {
		User user = user("test@example.com", userStatus);
		UserAuth userAuth = UserAuth.builder()
			.user(user)
			.provider(Provider.KAKAO)
			.providerUserId("social-id")
			.build();

		given(oAuthProvider.provider()).willReturn(Provider.KAKAO);
		given(oAuthProvider.getAccessToken("code", "redirect-uri")).willReturn("oauth-access-token");
		given(oAuthProvider.getUserInfo("oauth-access-token"))
			.willReturn(new OAuthUserInfoDTO("social-id", user.getEmail()));
		given(userAuthRepository.findByProviderAndProviderUserId(Provider.KAKAO, "social-id"))
			.willReturn(Optional.of(userAuth));

		assertThatThrownBy(() -> authService.socialLogin(Provider.KAKAO, "code", "redirect-uri"))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(errorCode);

		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	private void assertSocialEmailUserBlocked(UserStatus userStatus, ErrorCode errorCode) {
		User user = user("test@example.com", userStatus);

		given(oAuthProvider.provider()).willReturn(Provider.KAKAO);
		given(oAuthProvider.getAccessToken("code", "redirect-uri")).willReturn("oauth-access-token");
		given(oAuthProvider.getUserInfo("oauth-access-token"))
			.willReturn(new OAuthUserInfoDTO("social-id", user.getEmail()));
		given(userAuthRepository.findByProviderAndProviderUserId(Provider.KAKAO, "social-id"))
			.willReturn(Optional.empty());
		given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.socialLogin(Provider.KAKAO, "code", "redirect-uri"))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(errorCode);

		verify(userAuthRepository, never()).save(any(UserAuth.class));
	}

	private void assertTokenRefreshBlocked(UserStatus userStatus, ErrorCode errorCode) {
		User user = user("test@example.com", userStatus);
		given(jwtTokenProvider.parseRefreshToken("refresh-token"))
			.willReturn(new TokenClaims(user.getUserId(), "session-id", Instant.now().plusSeconds(3600)));
		given(userReader.readById(user.getUserId())).willReturn(user);

		assertThatThrownBy(() -> authService.tokenRefresh("refresh-token"))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(errorCode);

		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	private User user(String email, UserStatus userStatus) {
		return User.builder()
			.userId(1L)
			.email(email)
			.password("encoded-password")
			.nickname("nickname")
			.userStatus(userStatus)
			.lastAccessAt(LocalDateTime.now())
			.build();
	}
}
