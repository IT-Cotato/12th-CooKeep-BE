package com.cookeep.cookeep.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.request.AgreementRequestDTO;
import com.cookeep.cookeep.api.dto.user.UserProvider;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.onboarding.application.OnboardingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class OnboardingController {

	private final OnboardingService onboardingService;
	private final UserProvider userProvider;

	@Operation(summary = "약관 동의 여부 저장", description = "소셜로그인 회원을 대상으로 약관 동의 여부를 저장합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "약관 동의 여부 저장 성공"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@PatchMapping("/agreements")
	public ResponseEntity<DataResponse<?>> saveAgreement(
		@RequestBody AgreementRequestDTO agreementRequestDTO
		) {
		Long userId = userProvider.getCurrentUserId();
		onboardingService.saveAgreement(agreementRequestDTO, userId);
		return ResponseEntity.ok(DataResponse.ok());
	}
}
