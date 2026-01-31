package com.cookeep.cookeep.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.request.AgreementRequestDTO;
import com.cookeep.cookeep.api.dto.request.OnboardingRequestDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.security.UserPrincipal;
import com.cookeep.cookeep.domain.onboarding.application.OnboardingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class OnboardingController {

	private final OnboardingService onboardingService;

	@Operation(summary = "약관 동의 여부 저장", description = "소셜로그인 회원을 대상으로 약관 동의 여부를 저장합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "약관 동의 여부 저장 성공"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@PatchMapping("/agreements")
	public ResponseEntity<DataResponse<Void>> saveAgreement(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestBody AgreementRequestDTO agreementRequestDTO
		) {
		Long userId = principal.userId();
		onboardingService.saveAgreement(agreementRequestDTO, userId);
		return ResponseEntity.ok(DataResponse.ok());
	}

	@Operation(summary = "온보딩 내 알림 설정 업데이트", description = "온보딩 과정에서 회원이 마케팅 푸쉬 알람 수신에 동의할 경우 설정을 변경합니다")
	@PatchMapping("/onboarding/push")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "알림 설정 업데이트 성공"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	public ResponseEntity<DataResponse<Void>> agreeMarketingPush(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Long userId = principal.userId();
		onboardingService.agreeMarketingPush(userId);
		return ResponseEntity.ok(DataResponse.ok());
	}

	@Operation(summary = "온보딩 응답값 저장", description = "온보딩 과정에서 회원이 응답한 값을 저장합니다")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "온보딩 응답값 저장 성공"),
		@ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@PostMapping("/onboarding")
	public ResponseEntity<DataResponse<Void>> saveOnboarding(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody OnboardingRequestDTO onboardingRequestDTO
	) {
		Long userId = principal.userId();
		onboardingService.saveOnboarding(userId, onboardingRequestDTO);
		return ResponseEntity.ok(DataResponse.ok());
	}
}
