package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.RecipeLikeResponseDto;
import com.cookeep.cookeep.api.dto.response.WeeklyRecipeResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.dailyrecipe.application.RecipeBookmarkService;
import com.cookeep.cookeep.domain.dailyrecipe.application.RecipeLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "레시피 좋아요", description = "레시피 좋아요 관련 API")
@RestController
@RequestMapping("/api/daily-recipes/likes")
@RequiredArgsConstructor
public class RecipeLikeController {

	private final RecipeLikeService recipeLikeService;

	@Operation(
		summary = "레시피 좋아요 토글",
		description = "레시피에 좋아요를 추가하거나 취소합니다. 자신의 글에는 좋아요를 누를 수 없습니다."
	)
	@ApiErrorCodeExamples({
			ErrorCode.UNAUTHORIZED,
			ErrorCode.DAILY_RECIPE_NOT_FOUND,
			ErrorCode.CANNOT_LIKE_OWN_RECIPE
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
			@ApiResponse(responseCode = "400", description = "자신의 글에는 좋아요할 수 없음", content = @Content),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "404", description = "레시피를 찾을 수 없음", content = @Content)
	})
	@PostMapping("/{dailyRecipeId}/togle")
	public ResponseEntity<DataResponse<RecipeLikeResponseDto>> toggleLike(
		@AuthenticationPrincipal(expression = "userId") Long userId,
		@Parameter(description = "데일리 레시피 ID", required = true)
		@PathVariable Long dailyRecipeId
	) {
		boolean isLiked = recipeLikeService.toggleLike(userId, dailyRecipeId);
		long likeCount = recipeLikeService.getLikeCount(dailyRecipeId);

		RecipeLikeResponseDto response = RecipeLikeResponseDto.from(
			dailyRecipeId,
			isLiked,
			(int) likeCount
		);

		return ResponseEntity.ok(DataResponse.from(response));
	}

	@Operation(
		summary = "레시피 좋아요 여부 조회",
		description = "현재 사용자가 특정 레시피에 좋아요를 눌렀는지 확인합니다."
	)
	@ApiErrorCodeExamples({
			ErrorCode.UNAUTHORIZED,
			ErrorCode.DAILY_RECIPE_NOT_FOUND,
			ErrorCode.CANNOT_LIKE_OWN_RECIPE
	})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "좋아요 여부 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
		@ApiResponse(responseCode = "404", description = "레시피를 찾을 수 없음", content = @Content)
	})
	@GetMapping("/{dailyRecipeId}/check")
	public ResponseEntity<DataResponse<RecipeLikeResponseDto>> checkLike(
		@AuthenticationPrincipal(expression = "userId") Long userId,
		@Parameter(description = "데일리 레시피 ID", required = true)
		@PathVariable Long dailyRecipeId
	) {
		boolean isLiked = recipeLikeService.isLiked(userId, dailyRecipeId);
		long likeCount = recipeLikeService.getLikeCount(dailyRecipeId);

		RecipeLikeResponseDto response = RecipeLikeResponseDto.from(
			dailyRecipeId,
			isLiked,
			(int) likeCount
		);

		return ResponseEntity.ok(DataResponse.from(response));
	}

	@Operation(
			summary = "내가 좋아요 누른 레시피 목록 조회",
			description = "사용자가 좋아요를 누른 레시피들을 좋아요가 많은 순서대로 페이징 조회합니다."
	)@ApiErrorCodeExamples({
			ErrorCode.UNAUTHORIZED,
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
	})
	@GetMapping("/my")
	public ResponseEntity<DataResponse<Page<WeeklyRecipeResponseDto>>> getMyLikedRecipes(
			@AuthenticationPrincipal(expression = "userId") Long userId,
			@org.springdoc.core.annotations.ParameterObject Pageable pageable
	) {
		return ResponseEntity.ok(DataResponse.from(recipeLikeService.getMyLikedRecipes(userId, pageable)));
	}
}
