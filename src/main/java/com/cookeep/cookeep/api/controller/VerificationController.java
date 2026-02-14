package com.cookeep.cookeep.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.request.SendCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyCodeRequestDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.user.application.AuthService;
import com.cookeep.cookeep.domain.user.application.UserInfoService;
import com.cookeep.cookeep.security.UserPrincipal;

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
	private final UserInfoService userInfoService;

	// 회원가입 시 전화번호 인증 요청
	@Operation(summary = "회원가입 시 SMS 인증 요청 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류 (@Valid 검증 실패)"),
		@ApiResponse(responseCode = "409", description = "이미 사용중인 전화번호"),
		@ApiResponse(responseCode = "429", description = "인증 재요청이 너무 빠름"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping("/auth/signup/send-code")
	public ResponseEntity<DataResponse<Void>> sendSignupCode(
		@Valid @RequestBody SendCodeRequestDTO sendCodeRequestDTO
	) {
		authService.sendSignupCode(sendCodeRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}

	// 회원가입 전화번호 인증 확인
	@Operation(summary = "회원가입 시 SMS 인증 확인 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "확인 성공"),
		@ApiResponse(responseCode = "400", description = "요청 오류 (형식 오류 또는 인증 실패)"),
		@ApiResponse(responseCode = "404", description = "인증 요청 내역이 없음"),
		@ApiResponse(responseCode = "429", description = "인증 시도 횟수 초과"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping("/auth/signup/verify-code")
	public ResponseEntity<DataResponse<Void>> verifySignupCode(
		@Valid @RequestBody VerifyCodeRequestDTO verifyCodeRequestDTO
	) {
		authService.verifySignupCode(verifyCodeRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}

	// 비밀번호 찾기 시 전화번호 인증 요청
	@Operation(summary = "비밀번호 찾기 시 SMS 인증 요청 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류 (@Valid 검증 실패)"),
		@ApiResponse(responseCode = "404", description = "가입된 번호가 없음"),
		@ApiResponse(responseCode = "429", description = "인증 재요청이 너무 빠름"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping("/auth/password/send-code")
	public ResponseEntity<DataResponse<Void>> sendPasswordResetCode(
		@Valid @RequestBody SendCodeRequestDTO sendCodeRequestDTO
	) {
		authService.sendPasswordResetCode(sendCodeRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}

	// 비밀번호 찾기 시 전화번호 인증 확인
	@Operation(summary = "비밀번호 찾기 시 SMS 인증 확인 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "확인 성공"),
		@ApiResponse(responseCode = "400", description = "요청 오류 (형식 오류 또는 인증 실패)"),
		@ApiResponse(responseCode = "404", description = "인증 요청 내역이 없음"),
		@ApiResponse(responseCode = "429", description = "인증 시도 횟수 초과"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping("/auth/password/verify-code")
	public ResponseEntity<DataResponse<Void>> verifyPasswordResetCode(
		@Valid @RequestBody VerifyCodeRequestDTO verifyCodeRequestDTO
	) {
		authService.verifyPasswordResetCode(verifyCodeRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}

	// 전화번호 변경 시 전화번호 인증 요청
	@Operation(summary = "전화번호 변경 시 SMS 인증 요청 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "요청 오류 (@Valid 검증 실패)"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "403", description = "접근 권한 없음"),
		@ApiResponse(responseCode = "409", description = "이미 사용 중인 전화번호"),
		@ApiResponse(responseCode = "429", description = "인증 재요청이 너무 빠름"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping("/users/me/phone/send-code")
	public ResponseEntity<DataResponse<Void>> sendChangePhoneCode(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody SendCodeRequestDTO sendCodeRequestDTO
	) {
		Long userId = principal.userId();
		userInfoService.sendChangePhoneCode(userId, sendCodeRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}

	// 전화번호 변경 시 전화번호 인증 확인
	@Operation(summary = "전화번호 변경 시 SMS 인증 확인 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "확인 성공"),
		@ApiResponse(responseCode = "400", description = "요청 오류 (형식 오류 또는 인증 실패)"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "403", description = "접근 권한 없음"),
		@ApiResponse(responseCode = "404", description = "인증 요청 내역이 없음"),
		@ApiResponse(responseCode = "429", description = "인증 시도 횟수 초과"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping("/users/me/phone/verify-code")
	public ResponseEntity<DataResponse<Void>> verifyChangePhoneCode(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody VerifyCodeRequestDTO verifyCodeRequestDTO
	) {
		Long userId = principal.userId();
		userInfoService.verifyChangePhoneCode(userId, verifyCodeRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}

	// 비밀번호 검증 실패 시 전화번호 인증 요청
	@Operation(summary = "비밀번호 검증 실패 시 전화번호 인증 요청 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "요청 오류 (@Valid 검증 실패, 전화번호 불일치)"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "403", description = "접근 권한 없음"),
		@ApiResponse(responseCode = "429", description = "인증 재요청이 너무 빠름"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	@PostMapping("/users/me/password/send-code")
	public ResponseEntity<DataResponse<Void>> sendPasswordVerificationCode(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody SendCodeRequestDTO sendCodeRequestDTO
	) {
		Long userId = principal.userId();
		userInfoService.sendPasswordVerificationCode(userId, sendCodeRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}
}
