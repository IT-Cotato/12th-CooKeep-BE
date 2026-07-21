package com.cookeep.cookeep.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.response.LoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.api.dto.response.SocialLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.common.exception.GlobalExceptionHandler;
import com.cookeep.cookeep.domain.user.application.AuthService;
import com.cookeep.cookeep.domain.user.dto.AuthResult;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.cookeep.cookeep.security.RefreshTokenCookieProvider;
import com.cookeep.cookeep.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private AuthService authService;

	private AuthController authController;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		authController = new AuthController(authService, new RefreshTokenCookieProvider(true));
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void login_setsRefreshTokenCookieWithoutExposingItInBody() throws Exception {
		LoginResponseDTO response = new LoginResponseDTO(1L, "access-token", UserStatus.ACTIVE, false);
		given(authService.login(any(LoginRequestDTO.class)))
			.willReturn(new AuthResult<>(response, "refresh-token"));

		mockMvc.perform(post("/api/auth/login")
				.contentType("application/json")
				.content("""
					{"email":"test@example.com","password":"password1"}
					"""))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth/refresh")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist());
	}

	@Test
	void signup_setsRefreshTokenCookieWithoutExposingItInBody() throws Exception {
		SignUpResponseDTO response = new SignUpResponseDTO(1L, "access-token");
		given(authService.signUp(any(SignupRequestDTO.class)))
			.willReturn(new AuthResult<>(response, "refresh-token"));

		mockMvc.perform(post("/api/auth/signup")
				.contentType("application/json")
				.content("""
					{
					  "email":"test@example.com",
					  "password":"password1",
					  "passwordConfirm":"password1",
					  "marketingConsent":true
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist());
	}

	@Test
	void socialLogin_setsRefreshTokenCookieWithoutExposingItInBody() throws Exception {
		SocialLoginResponseDTO response = new SocialLoginResponseDTO(
			1L, "access-token", UserStatus.ACTIVE, null, false
		);
		given(authService.socialLogin(Provider.KAKAO, "code", null))
			.willReturn(new AuthResult<>(response, "refresh-token"));

		mockMvc.perform(get("/api/auth/login/kakao").param("code", "code"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist());
	}

	@Test
	void tokenRefresh_readsRefreshTokenFromCookieWithoutRequestBody() throws Exception {
		given(authService.tokenRefresh("refresh-token"))
			.willReturn(new TokenRefreshResponseDTO("new-access-token", false));

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(new MockCookie(RefreshTokenCookieProvider.COOKIE_NAME, "refresh-token")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

		verify(authService).tokenRefresh("refresh-token");
	}

	@Test
	void tokenRefresh_returnsUnauthorizedWhenCookieIsMissingOrInvalid() throws Exception {
		given(authService.tokenRefresh(null))
			.willThrow(new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
		given(authService.tokenRefresh("invalid-token"))
			.willThrow(new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

		mockMvc.perform(post("/api/auth/refresh"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REFRESH_TOKEN.getCode()));

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(new MockCookie(RefreshTokenCookieProvider.COOKIE_NAME, "invalid-token")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REFRESH_TOKEN.getCode()));
	}

	@Test
	void logout_andWithdrawExpireRefreshTokenCookie() {
		UserPrincipal principal = new UserPrincipal(1L);

		ResponseEntity<DataResponse<Void>> logoutResponse = authController.logout(principal);
		ResponseEntity<DataResponse<Void>> withdrawResponse = authController.withdraw(principal);

		assertExpiredRefreshTokenCookie(logoutResponse);
		assertExpiredRefreshTokenCookie(withdrawResponse);
		verify(authService).logout(1L);
		verify(authService).withdraw(1L);
	}

	private void assertExpiredRefreshTokenCookie(ResponseEntity<DataResponse<Void>> response) {
		String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

		assertThat(setCookie)
			.contains("refreshToken=")
			.contains("Path=/api/auth/refresh")
			.contains("Max-Age=0")
			.contains("HttpOnly")
			.contains("SameSite=Lax");
	}
}
