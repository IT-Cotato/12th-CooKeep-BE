package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.CookeepsOnboardingResponseDto;
import com.cookeep.cookeep.api.dto.response.CookeepsRecipeDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto;
import com.cookeep.cookeep.api.dto.response.CookeepsFeedResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.dto.SliceResponse;
import com.cookeep.cookeep.domain.cookeeps.application.CookeepsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "쿠킵스", description = "쿠킵스 관련 API")
@RestController
@RequestMapping("/api/cookeeps")
@RequiredArgsConstructor
public class CookeepsController {

	private final CookeepsService cookeepsService;

	@Operation(summary = "쿠킵스 랭킹 조회", description = "이번 달 물주기 횟수 Top 3 유저와 이번주 좋아요 Top 3 레시피를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	@GetMapping("/ranking")
	public ResponseEntity<DataResponse<RankingResponseDto>> getRanking(
			@AuthenticationPrincipal(expression = "userId") Long userId) {
		return ResponseEntity.ok(DataResponse.from(cookeepsService.getRanking(userId)));
	}

	@Operation(summary = "쿠킵스 온보딩 완료 여부 조회", description = "유저의 쿠킵스 온보딩 모달 확인 여부를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	@GetMapping("/onboarding")
	public ResponseEntity<DataResponse<CookeepsOnboardingResponseDto>> getOnboardingStatus(
			@AuthenticationPrincipal(expression = "userId") Long userId) {
		return ResponseEntity.ok(DataResponse.from(cookeepsService.getOnboardingStatus(userId)));
	}

	@Operation(summary = "쿠킵스 온보딩 완료 처리", description = "유저의 쿠킵스 온보딩 모달 확인 여부를 true로 업데이트합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "업데이트 성공")
	})
	@PatchMapping("/onboarding")
	public ResponseEntity<DataResponse<Void>> confirmOnboarding(
			@AuthenticationPrincipal(expression = "userId") Long userId) {
		cookeepsService.confirmOnboarding(userId);
		return ResponseEntity.ok(DataResponse.from(null));
	}

	@Operation(summary = "쿠킵스 공개 레시피 목록 전체 조회", description = "모든 유저의 공개 레시피를 정렬 필터와 함께 조회합니다. filter: latest(기본, 최신순), likes(좋아요 많은 순), oldest(오래된 순)")
	@GetMapping("/recipes")
	public ResponseEntity<DataResponse<SliceResponse<CookeepsFeedResponseDto>>> getAllRecipes(
			@RequestParam(defaultValue = "latest") String filter,
			@org.springdoc.core.annotations.ParameterObject Pageable pageable
	) {
		return ResponseEntity.ok(DataResponse.from(SliceResponse.from(cookeepsService.getAllRecipes(filter, pageable))));
	}

	@Operation(summary = "쿠킵스 레시피 상세 조회", description = "쿠킵스에 공개된 특정 레시피의 상세 내용을 조회합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "레시피를 찾을 수 없음")
	})
	@GetMapping("/recipes/{dailyRecipeId}")
	public ResponseEntity<DataResponse<CookeepsRecipeDetailResponseDto>> getCommunityRecipeDetail(
			@PathVariable Long dailyRecipeId
	) {
		return ResponseEntity.ok(DataResponse.from(cookeepsService.getCookeepsRecipeDetail(dailyRecipeId)));
	}
}
