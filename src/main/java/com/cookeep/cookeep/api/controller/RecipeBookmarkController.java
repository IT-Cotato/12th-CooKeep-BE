package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.CookeepsFeedResponseDto;
import com.cookeep.cookeep.api.dto.response.RecipeBookmarkResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.dto.SliceResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.dailyrecipe.application.RecipeBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "레시피 북마크", description = "레시피 북마크 관련 API")
@RestController
@RequestMapping("/api/daily-recipes/bookmarks")
@RequiredArgsConstructor
public class RecipeBookmarkController {
    private final RecipeBookmarkService recipeBookmarkService;

    @Operation(
            summary = "레시피 북마크 토글",
            description = "레시피를 북마크에 추가하거나 취소합니다. 자신의 글에는 북마크를 할 수 없습니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.DAILY_RECIPE_NOT_FOUND,
            ErrorCode.CANNOT_BOOKMARK_OWN_RECIPE
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 토글 성공 (isBookmarked 값으로 상태 확인)"),
            @ApiResponse(responseCode = "400", description = "본인 레시피 북마크 시도 시 발생", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 레시피가 존재하지 않음", content = @Content)
    })
    @PostMapping("/{dailyRecipeId}/toggle")
    public ResponseEntity<DataResponse<RecipeBookmarkResponseDto>> toggleBookmark(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "데일리 레시피 ID", required = true)
            @PathVariable Long dailyRecipeId
    ) {
        boolean isBookmarked = recipeBookmarkService.toggleBookmark(userId, dailyRecipeId);
        return ResponseEntity.ok(DataResponse.from(RecipeBookmarkResponseDto.from(dailyRecipeId, isBookmarked)));
    }

    @Operation(
            summary = "레시피 북마크 여부 조회",
            description = "현재 로그인한 사용자가 이 레시피를 북마크했는지 여부를 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.DAILY_RECIPE_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 레시피가 존재하지 않음", content = @Content)
    })
    @GetMapping("/{dailyRecipeId}/check")
    public ResponseEntity<DataResponse<RecipeBookmarkResponseDto>> checkBookmark(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "데일리 레시피 ID", required = true)
            @PathVariable Long dailyRecipeId
    ) {
        boolean isBookmarked = recipeBookmarkService.isBookmarked(userId, dailyRecipeId);
        return ResponseEntity.ok(DataResponse.from(RecipeBookmarkResponseDto.from(dailyRecipeId, isBookmarked)));
    }

    @Operation(
            summary = "내가 북마크한 레시피 목록 조회",
            description = "사용자가 북마크한 레시피들을 북마크 등록일 최신순으로 페이징 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/my")
    public ResponseEntity<DataResponse<SliceResponse<CookeepsFeedResponseDto>>> getMyBookmarkedRecipes(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(DataResponse.from(SliceResponse.from(recipeBookmarkService.getMyBookmarkedRecipes(userId, pageable))));
    }
}
