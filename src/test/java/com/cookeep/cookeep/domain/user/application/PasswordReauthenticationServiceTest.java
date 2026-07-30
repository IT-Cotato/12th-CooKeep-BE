package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cookeep.cookeep.api.dto.request.UpdatePasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyPasswordRequestDTO;
import com.cookeep.cookeep.api.dto.response.ReauthenticationResponseDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.dao.UserSessionRepository;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.ReauthenticationPurpose;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.verification.application.EmailVerificationService;

@ExtendWith(MockitoExtension.class)
class PasswordReauthenticationServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserReader userReader;
	@Mock
	private EmailVerificationService emailVerificationService;
	@Mock
	private UserAuthRepository userAuthRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private ProfileImageService profileImageService;
	@Mock
	private UserSessionRepository userSessionRepository;
	@Mock
	private ReauthenticationStore reauthenticationStore;

	private UserInfoService userInfoService;

	@BeforeEach
	void setUp() {
		ReauthenticationService reauthenticationService =
			new ReauthenticationService(reauthenticationStore);
		userInfoService = new UserInfoService(
			userRepository,
			userReader,
			emailVerificationService,
			userAuthRepository,
			passwordEncoder,
			profileImageService,
			userSessionRepository,
			reauthenticationService
		);
	}

	@Test
	void correctCurrentPasswordIssuesReauthenticationToken() {
		User user = user("encoded-old", 3);
		given(userReader.readById(1L)).willReturn(user);
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(passwordEncoder.matches("old-password1", "encoded-old")).willReturn(true);
		given(reauthenticationStore.issue(1L, ReauthenticationPurpose.CHANGE_PASSWORD))
			.willReturn("reauth-token");

		ReauthenticationResponseDTO response = userInfoService.verifyMyPassword(
			1L,
			new VerifyPasswordRequestDTO("old-password1")
		);

		assertThat(response.reauthToken()).isEqualTo("reauth-token");
		assertThat(response.expiresInSeconds()).isEqualTo(300);
		assertThat(user.getPasswordCnt()).isZero();
	}

	@Test
	void wrongCurrentPasswordDoesNotIssueToken() {
		User user = user("encoded-old", 0);
		given(userReader.readById(1L)).willReturn(user);
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(passwordEncoder.matches("wrong-password", "encoded-old")).willReturn(false);

		assertThatThrownBy(() -> userInfoService.verifyMyPassword(
			1L,
			new VerifyPasswordRequestDTO("wrong-password")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PASSWORD_MISMATCH);

		assertThat(user.getPasswordCnt()).isEqualTo(1);
		verify(reauthenticationStore, never()).issue(any(), any());
	}

	@Test
	void socialUserCannotIssueToken() {
		User user = user(null, null);
		given(userReader.readById(1L)).willReturn(user);
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(false);

		assertThatThrownBy(() -> userInfoService.verifyMyPassword(
			1L,
			new VerifyPasswordRequestDTO("password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SOCIAL_USER_PASSWORD_CHANGE_NOT_ALLOWED);

		verify(passwordEncoder, never()).matches(any(), any());
		verify(reauthenticationStore, never()).issue(any(), any());
	}

	@Test
	void redisIssueFailureDoesNotIssueTokenOrResetFailureCount() {
		User user = user("encoded-old", 2);
		given(userReader.readById(1L)).willReturn(user);
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(passwordEncoder.matches("old-password1", "encoded-old")).willReturn(true);
		given(reauthenticationStore.issue(1L, ReauthenticationPurpose.CHANGE_PASSWORD))
			.willThrow(new AppException(ErrorCode.REAUTHENTICATION_UNAVAILABLE));

		assertThatThrownBy(() -> userInfoService.verifyMyPassword(
			1L,
			new VerifyPasswordRequestDTO("old-password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.REAUTHENTICATION_UNAVAILABLE);

		assertThat(user.getPasswordCnt()).isEqualTo(2);
	}

	@Test
	void missingTokenCannotChangePassword() {
		assertThatThrownBy(() -> userInfoService.updateMyPassword(
			1L,
			" ",
			updateRequest("new-password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.REAUTH_TOKEN_REQUIRED);

		verify(userRepository, never()).findByIdForUpdate(any());
	}

	@Test
	void invalidTokenCannotChangePassword() {
		User user = user("encoded-old", 2);
		given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(passwordEncoder.matches("new-password1", "encoded-old")).willReturn(false);
		org.mockito.Mockito.doThrow(new AppException(ErrorCode.INVALID_REAUTH_TOKEN))
			.when(reauthenticationStore)
			.consume(1L, ReauthenticationPurpose.CHANGE_PASSWORD, "invalid-token");

		assertThatThrownBy(() -> userInfoService.updateMyPassword(
			1L,
			"invalid-token",
			updateRequest("new-password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REAUTH_TOKEN);

		assertThat(user.getPassword()).isEqualTo("encoded-old");
		verify(passwordEncoder, never()).encode(any());
		verify(userSessionRepository, never()).deleteByUser_UserId(any());
	}

	@Test
	void socialUserCannotChangePassword() {
		User user = user(null, null);
		given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(false);

		assertThatThrownBy(() -> userInfoService.updateMyPassword(
			1L,
			"reauth-token",
			updateRequest("new-password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SOCIAL_USER_PASSWORD_CHANGE_NOT_ALLOWED);

		verify(reauthenticationStore, never()).consume(any(), any(), any());
	}

	@Test
	void samePasswordIsRejectedWithoutConsumingToken() {
		User user = user("encoded-old", 2);
		given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(passwordEncoder.matches("old-password1", "encoded-old")).willReturn(true);

		assertThatThrownBy(() -> userInfoService.updateMyPassword(
			1L,
			"reauth-token",
			updateRequest("old-password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SAME_AS_PREVIOUS_PASSWORD);

		verify(reauthenticationStore, never()).consume(any(), any(), any());
	}

	@Test
	void successfulChangeUpdatesPasswordResetsCountAndDeletesRefreshSession() {
		User user = user("encoded-old", 4);
		given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(passwordEncoder.matches("new-password1", "encoded-old")).willReturn(false);
		given(passwordEncoder.encode("new-password1")).willReturn("encoded-new");

		userInfoService.updateMyPassword(
			1L,
			"reauth-token",
			updateRequest("new-password1")
		);

		verify(reauthenticationStore).consume(
			1L,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			"reauth-token"
		);
		assertThat(user.getPassword()).isEqualTo("encoded-new");
		assertThat(user.getPasswordCnt()).isZero();
		verify(userSessionRepository).deleteByUser_UserId(1L);
	}

	@Test
	void redisConsumeFailureDoesNotChangePasswordOrDeleteSession() {
		User user = user("encoded-old", 2);
		given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));
		given(userAuthRepository.existsByUser_UserIdAndProvider(1L, Provider.LOCAL))
			.willReturn(true);
		given(passwordEncoder.matches("new-password1", "encoded-old")).willReturn(false);
		org.mockito.Mockito.doThrow(new AppException(ErrorCode.REAUTHENTICATION_UNAVAILABLE))
			.when(reauthenticationStore)
			.consume(1L, ReauthenticationPurpose.CHANGE_PASSWORD, "reauth-token");

		assertThatThrownBy(() -> userInfoService.updateMyPassword(
			1L,
			"reauth-token",
			updateRequest("new-password1")
		))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.REAUTHENTICATION_UNAVAILABLE);

		assertThat(user.getPassword()).isEqualTo("encoded-old");
		verify(passwordEncoder, never()).encode(any());
		verify(userSessionRepository, never()).deleteByUser_UserId(any());
	}

	private UpdatePasswordRequestDTO updateRequest(String password) {
		return new UpdatePasswordRequestDTO(password, password);
	}

	private User user(String password, Integer passwordCnt) {
		return User.builder()
			.userId(1L)
			.email("test@example.com")
			.nickname("nickname")
			.password(password)
			.passwordCnt(passwordCnt)
			.build();
	}
}
