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
	@Operation(summary = "회원가입 시 SMS 인증 요청 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 전화번호"),
		@ApiResponse(responseCode = "429", description = "인증 재요청이 너무 빠름"),
		@ApiResponse(responseCode = "500", description = "SMS 발송 실패 (외부 서비스 오류)")
	})
	@PostMapping("/auth/signup/send-code")
	public DataResponse<Void> sendSignupCode(
		@Valid @RequestBody SendCodeRequestDTO sendCodeRequestDTO
	) {
		authService.sendSignupCode(sendCodeRequestDTO);
		return DataResponse.ok();
	}

	// 비밀번호 찾기 시 전화번호 인증 요청
	@Operation(summary = "비밀번호 찾기 시 SMS 인증 요청 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "409", description = "가입된 번호가 없음"),
		@ApiResponse(responseCode = "429", description = "인증 재요청이 너무 빠름"),
		@ApiResponse(responseCode = "500", description = "SMS 발송 실패 (외부 서비스 오류)")
	})
	@PostMapping("/auth/password/send-code")
	public DataResponse<Void> sendPasswordResetCode(
		@Valid @RequestBody SendCodeRequestDTO sendCodeRequestDTO
	) {
		authService.sendPasswordResetCode(sendCodeRequestDTO);
		return DataResponse.ok();
	}

	// 회원가입, 비밀번호 찾기 시 전화번호 인증 확인
	// 요구사항이 동일하므로 동일한 api 사용하도록 하였음
	@Operation(summary = "회원가입, 비밀번호 찾기 시 SMS 인증 확인 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "확인 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "404", description = "인증 요청 내역이 없음"),
		@ApiResponse(responseCode = "409", description = "인증번호 불일치 또는 만료됨"),
		@ApiResponse(responseCode = "429", description = "인증 시도 횟수 초과")
	})
	@PostMapping("/auth/verify-code")
	public DataResponse<Void> verifyAuthCode(
		@Valid @RequestBody VerifyCodeRequestDTO verifyCodeRequestDTO
	) {
		authService.verifyAuthCode(verifyCodeRequestDTO);
		return DataResponse.ok();
	}
}
