package com.cookeep.cookeep.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.request.TokenRefreshRequestDTO;
import com.cookeep.cookeep.api.dto.response.KakaoLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.LoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.user.application.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "액세스 토큰 재발급 API")
	@PostMapping("/refresh")
	public ResponseEntity<DataResponse<TokenRefreshResponseDTO>> tokenRefresh(@RequestBody TokenRefreshRequestDTO tokenRefreshRequestDTO) {
		return ResponseEntity.ok(DataResponse.from(authService.tokenRefresh(tokenRefreshRequestDTO)));

	}

	@Operation(summary = "카카오 로그인 API")
	@GetMapping("/login/kakao")
	public ResponseEntity<DataResponse<KakaoLoginResponseDTO>> kakaoLogin(@RequestParam String code) {
		return ResponseEntity.ok(DataResponse.from(authService.kakaoLogin(code)));
	}

	@Operation(summary = "전화번호 회원가입 API")
	@PostMapping("/signup")
	public ResponseEntity<DataResponse<SignUpResponseDTO>> signup(@Valid @RequestBody SignupRequestDTO signupRequestDTO) {
		return ResponseEntity.ok(DataResponse.from(authService.signUp(signupRequestDTO)));
	}

	@Operation(summary = "전화번호 로그인 API")
	@PostMapping("/login")
	public ResponseEntity<DataResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
		return ResponseEntity.ok(DataResponse.from(authService.login(loginRequestDTO)));
	}
}
