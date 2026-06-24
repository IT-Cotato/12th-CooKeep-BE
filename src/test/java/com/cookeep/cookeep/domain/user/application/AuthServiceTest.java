package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.dao.UserSessionRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserAuth;
import com.cookeep.cookeep.domain.user.entity.UserSession;
import com.cookeep.cookeep.domain.verification.application.EmailVerificationService;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;
import com.cookeep.cookeep.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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
	private PasswordEncoder passwordEncoder;

	@Mock
	private NicknameGenerator nicknameGenerator;

	@Mock
	private EmailVerificationService emailVerificationService;

	@Mock
	private CookieService cookieService;

	@Mock
	private WebPushSubscriptionRepository webPushSubscriptionRepository;

	@Mock
	private List<OAuthProvider> oAuthProviders;

	@InjectMocks
	private AuthService authService;

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
		given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
		given(userSessionRepository.findByUser(savedUser)).willReturn(Optional.empty());
		given(userSessionRepository.save(any(UserSession.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(userAuthRepository.save(any(UserAuth.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignUpResponseDTO response = authService.signUp(request);

		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		verify(emailVerificationService).assertVerified(email, VerificationPurpose.SIGNUP);
		verify(userRepository).saveAndFlush(any(User.class));
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
}
