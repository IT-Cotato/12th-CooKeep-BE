package com.cookeep.cookeep.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.cookeep.cookeep.api.dto.request.TokenRefreshRequestDTO;
import com.cookeep.cookeep.api.dto.response.SocialLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.LoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.user.application.AuthService;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "액세스 토큰 재발급 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "액세스 토큰 갱신 성공"),
		@ApiResponse(responseCode = "401", description = "유효하지 않은 리프레쉬 토큰")
	})
	@PostMapping("/refresh")
	public ResponseEntity<DataResponse<TokenRefreshResponseDTO>> tokenRefresh(@RequestBody TokenRefreshRequestDTO tokenRefreshRequestDTO) {
		return ResponseEntity.ok(DataResponse.from(authService.tokenRefresh(tokenRefreshRequestDTO)));

	}

	@Operation(summary = "카카오 로그인 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "카카오 로그인 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
	})
	@GetMapping("/login/kakao")
	public ResponseEntity<DataResponse<SocialLoginResponseDTO>> kakaoLogin(
		@RequestParam String code,
		@RequestParam(value = "redirect_uri", required = false) String redirectUri) {
		return ResponseEntity.ok(DataResponse.from(authService.socialLogin(Provider.KAKAO, code, redirectUri)));
	}

	@Operation(summary = "구글 로그인 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구글 로그인 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
	})
	@GetMapping("/login/google")
	public ResponseEntity<DataResponse<SocialLoginResponseDTO>> googleLogin(
		@RequestParam String code,
		@RequestParam(value = "redirect_uri", required = false) String redirectUri) {
		return ResponseEntity.ok(DataResponse.from(authService.socialLogin(Provider.GOOGLE, code, redirectUri)));
	}

	@Operation(summary = "전화번호 회원가입 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원가입 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 전화번호 또는 이메일")
	})
	@PostMapping("/signup")
	public ResponseEntity<DataResponse<SignUpResponseDTO>> signup(@Valid @RequestBody SignupRequestDTO signupRequestDTO) {
		return ResponseEntity.ok(DataResponse.from(authService.signUp(signupRequestDTO)));
	}

	@Operation(summary = "전화번호 로그인 API")
	@PostMapping("/login")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원가입 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
	})
	public ResponseEntity<DataResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
		return ResponseEntity.ok(DataResponse.from(authService.login(loginRequestDTO)));
	}

	// @Operation(summary = "비밀번호 초기화 API")
	// @ApiResponses(value = {
	// 	@ApiResponse(responseCode = "200", description = "비밀번호 초기화 성공"),
	// 	@ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
	// })
	// @PatchMapping("/password/reset")
	// public ResponseEntity<DataResponse<Void>> resetPassword(
	// 	@Valid @RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO
	// ) {
	// 	authService.resetPassword(resetPasswordRequestDTO);
	// 	return ResponseEntity.ok(DataResponse.ok());
	// }

	@Operation(summary = "로그아웃 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음")
	})
	@PostMapping("/logout")
	public ResponseEntity<DataResponse<Void>> logout(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = principal.userId();
		authService.logout(userId);
		return ResponseEntity.ok(DataResponse.ok());
	}

	@Operation(summary = "회원 탈퇴 API", description = "현재 로그인한 사용자를 탈퇴 처리합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@DeleteMapping("/withdraw")
	public ResponseEntity<DataResponse<Void>> withdraw(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = principal.userId();
		authService.withdraw(userId);
		return ResponseEntity.ok(DataResponse.ok());
	}
}
