package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.UpdatePasswordRequestDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.dao.UserSessionRepository;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserSession;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.cookeep.cookeep.domain.verification.application.EmailVerificationService;
import com.cookeep.cookeep.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class PasswordChangeLoginFlowTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserAuthRepository userAuthRepository;
	@Mock
	private UserSessionRepository userSessionRepository;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private UserReader userReader;
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
	@Mock
	private ProfileImageService profileImageService;
	@Mock
	private ReauthenticationStore reauthenticationStore;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private UserInfoService userInfoService;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		userInfoService = new UserInfoService(
			userRepository,
			userReader,
			emailVerificationService,
			userAuthRepository,
			passwordEncoder,
			profileImageService,
			userSessionRepository,
			new ReauthenticationService(reauthenticationStore)
		);
		authService = new AuthService(
			userRepository,
			userAuthRepository,
			userSessionRepository,
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
	void oldPasswordCannotLoginAndNewPasswordCanLoginAfterChange() {
		User user = User.builder()
			.userId(1L)
			.email("test@example.com")
			.nickname("nickname")
			.password(passwordEncoder.encode("old-password1"))
			.passwordCnt(2)
			.userStatus(UserStatus.ACTIVE)
			.lastAccessAt(LocalDateTime.now())
			.build();
		given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
		given(loginPasswordFailureService.increasePasswordFailCount(1L)).willReturn(1);
		given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
		given(userSessionRepository.findByUser(user)).willReturn(Optional.empty());
		given(userSessionRepository.save(any(UserSession.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		userInfoService.updateMyPassword(
			1L,
			"reauth-token",
			new UpdatePasswordRequestDTO("new-password1", "new-password1")
		);

		assertThatThrownBy(() -> authService.login(
			new LoginRequestDTO("test@example.com", "old-password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.AUTH_PASSWORD_MISMATCH);

		var loginResult = authService.login(
			new LoginRequestDTO("test@example.com", "new-password1")
		);

		assertThat(loginResult.response().accessToken()).isEqualTo("access-token");
		assertThat(loginResult.refreshToken()).isEqualTo("refresh-token");
	}
}
