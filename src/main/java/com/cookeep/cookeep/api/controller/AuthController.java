package com.cookeep.cookeep.api.controller;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.ResetPasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.response.LoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.api.dto.response.SocialLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.user.application.AuthService;
import com.cookeep.cookeep.domain.user.dto.AuthResult;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.security.RefreshTokenCookieProvider;
import com.cookeep.cookeep.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private static final String REFRESH_COOKIE_ISSUE_DESCRIPTION = """
		refreshToken은 응답 body에 포함되지 않고 Set-Cookie 응답 헤더의 HttpOnly Cookie로 전달됨
		""";

	private static final String REFRESH_COOKIE_DELETE_DESCRIPTION = """
		성공 시 refreshToken HttpOnly Cookie 만료됨
		""";

	private static final String REFRESH_COOKIE_SET_COOKIE_HEADER = """
		refreshToken HttpOnly Cookie. Path=/api/auth/refresh
		""";

	private static final String REFRESH_COOKIE_DELETE_HEADER = """
		refreshToken 만료 Cookie. Path=/api/auth/refresh
		""";

	private final AuthService authService;
	private final RefreshTokenCookieProvider refreshTokenCookieProvider;

	@Operation(
		summary = "액세스 토큰 재발급 API",
		description = """
			request body 없이 refreshToken HttpOnly Cookie로 Access Token과 Refresh Token을 재발급합니다.
			성공 시 회전된 Refresh Token을 Set-Cookie 헤더로 전달하며 최초 로그인 기준 절대 만료시간은 연장하지 않습니다.
			쿠키 누락·위조·만료·세션 없음·다른 로그인 세션은 401 AUTH-002로 처리합니다.
			이미 회전된 Refresh Token의 재사용은 401 AUTH-015로 처리하고 현재 인증 세션을 폐기합니다.
			"""
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Access Token 및 Refresh Token 갱신 성공",
			headers = @Header(
				name = HttpHeaders.SET_COOKIE,
				description = "회전된 Refresh Token HttpOnly Cookie",
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "유효하지 않은 Refresh Token(AUTH-002) 또는 재사용 탐지(AUTH-015)"
		),
		@ApiResponse(responseCode = "503", description = "Redis 인증 세션 서비스 사용 불가(AUTH-016)")
	})
	@PostMapping("/refresh")
	public ResponseEntity<DataResponse<TokenRefreshResponseDTO>> tokenRefresh(
		@Parameter(
			name = RefreshTokenCookieProvider.COOKIE_NAME,
			in = ParameterIn.COOKIE,
			required = true,
			description = "로그인/회원가입/소셜 로그인 응답의 Set-Cookie 헤더로 저장된 refreshToken HttpOnly Cookie",
			example = "eyJhbGciOiJIUzI1NiJ9..."
		)
		@CookieValue(value = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
		return createAuthResponse(authService.tokenRefresh(refreshToken));
	}

	@Operation(summary = "카카오 로그인 API", description = REFRESH_COOKIE_ISSUE_DESCRIPTION)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "카카오 로그인 성공",
			headers = @Header(
				name = "Set-Cookie",
				description = REFRESH_COOKIE_SET_COOKIE_HEADER,
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
	})
	@GetMapping("/login/kakao")
	public ResponseEntity<DataResponse<SocialLoginResponseDTO>> kakaoLogin(
		@RequestParam String code,
		@RequestParam(value = "redirect_uri", required = false) String redirectUri) {
		return createAuthResponse(authService.socialLogin(Provider.KAKAO, code, redirectUri));
	}

	@Operation(summary = "구글 로그인 API", description = REFRESH_COOKIE_ISSUE_DESCRIPTION)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "구글 로그인 성공",
			headers = @Header(
				name = "Set-Cookie",
				description = REFRESH_COOKIE_SET_COOKIE_HEADER,
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
	})
	@GetMapping("/login/google")
	public ResponseEntity<DataResponse<SocialLoginResponseDTO>> googleLogin(
		@RequestParam String code,
		@RequestParam(value = "redirect_uri", required = false) String redirectUri) {
		return createAuthResponse(authService.socialLogin(Provider.GOOGLE, code, redirectUri));
	}

	@Operation(summary = "이메일 회원가입 API", description = REFRESH_COOKIE_ISSUE_DESCRIPTION)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "회원가입 성공",
			headers = @Header(
				name = "Set-Cookie",
				description = REFRESH_COOKIE_SET_COOKIE_HEADER,
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 전화번호 또는 이메일")
	})
	@PostMapping("/signup")
	public ResponseEntity<DataResponse<SignUpResponseDTO>> signup(@Valid @RequestBody SignupRequestDTO signupRequestDTO) {
		return createAuthResponse(authService.signUp(signupRequestDTO));
	}

	@Operation(summary = "이메일 로그인 API", description = REFRESH_COOKIE_ISSUE_DESCRIPTION)
	@PostMapping("/login")
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "로그인 성공",
			headers = @Header(
				name = "Set-Cookie",
				description = REFRESH_COOKIE_SET_COOKIE_HEADER,
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
	})
	public ResponseEntity<DataResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
		return createAuthResponse(authService.login(loginRequestDTO));
	}

	@Operation(summary = "비밀번호 초기화 API")
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "비밀번호 초기화 및 Refresh Cookie 만료 성공",
			headers = @Header(
				name = HttpHeaders.SET_COOKIE,
				description = REFRESH_COOKIE_DELETE_HEADER,
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "503", description = "Redis 인증 세션 서비스 사용 불가(AUTH-016)")
	})
	@PatchMapping("/password/reset")
	public ResponseEntity<DataResponse<Void>> resetPassword(
		@Valid @RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO
	) {
		authService.resetPassword(resetPasswordRequestDTO);
		return createLogoutResponse();
	}

	@Operation(summary = "로그아웃 API", description = REFRESH_COOKIE_DELETE_DESCRIPTION)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "로그아웃 성공",
			headers = @Header(
				name = "Set-Cookie",
				description = REFRESH_COOKIE_DELETE_HEADER,
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음")
	})
	@PostMapping("/logout")
	public ResponseEntity<DataResponse<Void>> logout(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = principal.userId();
		authService.logout(userId);
		return createLogoutResponse();
	}

	@Operation(summary = "회원 탈퇴 API", description = "현재 로그인한 사용자를 탈퇴 처리함\n" + REFRESH_COOKIE_DELETE_DESCRIPTION)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "회원 탈퇴 성공",
			headers = @Header(
				name = "Set-Cookie",
				description = REFRESH_COOKIE_DELETE_HEADER,
				schema = @Schema(type = "string")
			)
		),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@DeleteMapping("/withdraw")
	public ResponseEntity<DataResponse<Void>> withdraw(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = principal.userId();
		authService.withdraw(userId);
		return createLogoutResponse();
	}

	private <T> ResponseEntity<DataResponse<T>> createAuthResponse(AuthResult<T> authResult) {
		// refresh token은 응답 body에 노출하지 않고 브라우저의 HttpOnly cookie로만 전달한다.
		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				refreshTokenCookieProvider.create(
					authResult.refreshToken(),
					Duration.ofSeconds(authResult.refreshExpiresInSeconds())
				).toString()
			)
			.body(DataResponse.from(authResult.response()));
	}

	private ResponseEntity<DataResponse<Void>> createLogoutResponse() {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.delete().toString())
			.body(DataResponse.ok());
	}
}
