package com.cookeep.cookeep.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.request.SendCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyCodeRequestDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.user.application.AuthService;
import com.cookeep.cookeep.domain.user.application.UserInfoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VerificationController {

	private final AuthService authService;

	// 회원가입 시 전화번호 인증 요청
	@Operation(summary = "회원가입 시 전화번호 SMS 인증 요청 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 전화번호"),
		@ApiResponse(responseCode = "429", description = "인증 재요청이 너무 빠르거나 인증 시도 횟수를 초과함")
	})
	@PostMapping("/auth/signup/send-code")
	public DataResponse<Void> sendSignupCode(
		@Valid @RequestBody SendCodeRequestDTO sendCodeRequestDTO
	) {
		authService.sendSignupCode(sendCodeRequestDTO.phoneNumber());
		return DataResponse.ok();
	}

	// 회원가입 시 전화번호 인증 확인
	@PostMapping("/auth/signup/verify-code")
	@Operation(summary = "회원가입 시 전화번호 SMS 인증 확인 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "확인 성공")
	})
	public DataResponse<Void> verifySignupCode(
		@Valid @RequestBody VerifyCodeRequestDTO verifyCodeRequestDTO
	) {
		authService.verifySignupCode(verifyCodeRequestDTO.phoneNumber(), verifyCodeRequestDTO.code());
		return DataResponse.ok();
	}
}
