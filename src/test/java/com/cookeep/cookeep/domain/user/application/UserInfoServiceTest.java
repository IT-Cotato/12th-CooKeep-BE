package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cookeep.cookeep.api.dto.request.SendCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyCodeRequestDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.verification.application.EmailVerificationService;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceTest {

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

	@InjectMocks
	private UserInfoService userInfoService;

	@Test
	@DisplayName("sendChangeEmailCode sends CHANGE_EMAIL code when new email is available")
	void sendChangeEmailCode_availableEmail_sendsCode() {
		User user = user("current@example.com");
		given(userReader.readById(1L)).willReturn(user);
		given(userRepository.existsByEmail("new@example.com")).willReturn(false);

		userInfoService.sendChangeEmailCode(1L, new SendCodeRequestDTO("new@example.com"));

		verify(emailVerificationService).sendCode("new@example.com", VerificationPurpose.CHANGE_EMAIL);
	}

	@Test
	@DisplayName("sendChangeEmailCode rejects current email")
	void sendChangeEmailCode_currentEmail_throwsException() {
		User user = user("current@example.com");
		given(userReader.readById(1L)).willReturn(user);

		assertThatThrownBy(() -> userInfoService.sendChangeEmailCode(1L, new SendCodeRequestDTO("current@example.com")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SAME_AS_CURRENT_EMAIL);

		verify(emailVerificationService, never()).sendCode(any(), any());
	}

	@Test
	@DisplayName("sendChangeEmailCode rejects duplicate email")
	void sendChangeEmailCode_duplicateEmail_throwsException() {
		User user = user("current@example.com");
		given(userReader.readById(1L)).willReturn(user);
		given(userRepository.existsByEmail("new@example.com")).willReturn(true);

		assertThatThrownBy(() -> userInfoService.sendChangeEmailCode(1L, new SendCodeRequestDTO("new@example.com")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.USER_EMAIL_ALREADY_EXISTS);

		verify(emailVerificationService, never()).sendCode(any(), any());
	}

	@Test
	@DisplayName("verifyChangeEmailCode updates email after CHANGE_EMAIL verification succeeds")
	void verifyChangeEmailCode_verifiedCode_updatesEmail() {
		User user = user("current@example.com");
		given(userAuthRepository.findProviderByUserId(1L)).willReturn(Provider.LOCAL);
		given(userReader.readById(1L)).willReturn(user);
		given(userRepository.existsByEmail("new@example.com")).willReturn(false);

		userInfoService.verifyChangeEmailCode(1L, new VerifyCodeRequestDTO("new@example.com", "123456"));

		verify(emailVerificationService).verifyCode("new@example.com", VerificationPurpose.CHANGE_EMAIL, "123456");
		assertThat(user.getEmail()).isEqualTo("new@example.com");
	}

	@Test
	@DisplayName("verifyChangeEmailCode rejects social user")
	void verifyChangeEmailCode_socialUser_throwsException() {
		given(userAuthRepository.findProviderByUserId(1L)).willReturn(Provider.KAKAO);

		assertThatThrownBy(() -> userInfoService.verifyChangeEmailCode(1L, new VerifyCodeRequestDTO("new@example.com", "123456")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SOCIAL_USER_EMAIL_CHANGE_NOT_ALLOWED);

		verify(userReader, never()).readById(any());
		verify(emailVerificationService, never()).verifyCode(any(), any(), any());
	}

	@Test
	@DisplayName("verifyChangeEmailCode rejects current email")
	void verifyChangeEmailCode_currentEmail_throwsException() {
		User user = user("current@example.com");
		given(userAuthRepository.findProviderByUserId(1L)).willReturn(Provider.LOCAL);
		given(userReader.readById(1L)).willReturn(user);

		assertThatThrownBy(() -> userInfoService.verifyChangeEmailCode(1L, new VerifyCodeRequestDTO("current@example.com", "123456")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SAME_AS_CURRENT_EMAIL);

		verify(emailVerificationService, never()).verifyCode(any(), any(), any());
	}

	@Test
	@DisplayName("verifyChangeEmailCode rejects duplicate email")
	void verifyChangeEmailCode_duplicateEmail_throwsException() {
		User user = user("current@example.com");
		given(userAuthRepository.findProviderByUserId(1L)).willReturn(Provider.LOCAL);
		given(userReader.readById(1L)).willReturn(user);
		given(userRepository.existsByEmail("new@example.com")).willReturn(true);

		assertThatThrownBy(() -> userInfoService.verifyChangeEmailCode(1L, new VerifyCodeRequestDTO("new@example.com", "123456")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.USER_EMAIL_ALREADY_EXISTS);

		verify(emailVerificationService, never()).verifyCode(any(), any(), any());
	}

	@Test
	@DisplayName("verifyChangeEmailCode propagates verification failure")
	void verifyChangeEmailCode_verificationFails_doesNotUpdateEmail() {
		User user = user("current@example.com");
		given(userAuthRepository.findProviderByUserId(1L)).willReturn(Provider.LOCAL);
		given(userReader.readById(1L)).willReturn(user);
		given(userRepository.existsByEmail("new@example.com")).willReturn(false);
		doThrow(new AppException(ErrorCode.VERIFICATION_CODE_EXPIRED))
			.when(emailVerificationService)
			.verifyCode("new@example.com", VerificationPurpose.CHANGE_EMAIL, "123456");

		assertThatThrownBy(() -> userInfoService.verifyChangeEmailCode(1L, new VerifyCodeRequestDTO("new@example.com", "123456")))
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.VERIFICATION_CODE_EXPIRED);

		assertThat(user.getEmail()).isEqualTo("current@example.com");
	}

	private User user(String email) {
		return User.builder()
			.userId(1L)
			.email(email)
			.nickname("nickname")
			.build();
	}
}
