package com.cookeep.cookeep.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.request.TokenRefreshRequestDTO;
import com.cookeep.cookeep.api.dto.response.KakaoLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.user.application.AuthService;

import io.swagger.v3.oas.annotations.Operation;
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
	public ResponseEntity<DataResponse<KakaoLoginResponseDTO>> kakaoLogin(
			@RequestParam String code,
			@RequestParam("redirect_uri") String redirectUri) {
		return ResponseEntity.ok(DataResponse.from(authService.kakaoLogin(code, redirectUri)));
	}
}
