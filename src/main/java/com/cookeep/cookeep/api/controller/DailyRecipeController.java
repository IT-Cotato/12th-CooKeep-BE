package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.DailyRecipeCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.AdoptedAiRecipeDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.AdoptedAiRecipeListResponseDto;
import com.cookeep.cookeep.api.dto.response.DailyRecipeCreateResponseDto;
import com.cookeep.cookeep.api.dto.response.DailyRecipeDetailResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.dailyrecipe.application.DailyRecipeService;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(
        name = "데일리 레시피",
        description = "데일리 요리 기록 등록/조회 API"
)
@RestController
@RequestMapping("/api/users/me/daily-recipes")
@RequiredArgsConstructor
public class DailyRecipeController {

    private final DailyRecipeService dailyRecipeService;

    @Operation(
            summary = "채택된 AI 레시피 목록 조회",
            description = "데일리 레시피 등록을 위한 채택된 AI 레시피 목록을 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채택된 AI 레시피 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/ai-recipes")
    public ResponseEntity<DataResponse<List<AdoptedAiRecipeListResponseDto>>> getAdoptedAiRecipes(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        List<AiRecipe> aiRecipes = dailyRecipeService.getAdoptedAiRecipes(userId);
        List<AdoptedAiRecipeListResponseDto> response = aiRecipes.stream()
                .map(AdoptedAiRecipeListResponseDto::from)
                .toList();

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "채택된 AI 레시피 상세 조회",
            description = "원하는 레시피 선택 후 해당 AI 레시피의 상세 내용(재료, 조리단계, 유튜브)을 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.AI_RECIPE_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 레시피 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "AI 레시피를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/ai-recipes/{aiRecipeId}")
    public ResponseEntity<DataResponse<AdoptedAiRecipeDetailResponseDto>> getAdoptedAiRecipeDetail(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "AI 레시피 ID", required = true)
            @PathVariable Long aiRecipeId
    ) {
        AiRecipe aiRecipe = dailyRecipeService.getAdoptedAiRecipeDetail(userId, aiRecipeId);

        return ResponseEntity.ok(DataResponse.from(AdoptedAiRecipeDetailResponseDto.from(aiRecipe)));
    }

    @Operation(
            summary = "데일리 레시피 등록",
            description = "채택된 AI 레시피를 기반으로 데일리 요리를 기록합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.AI_RECIPE_NOT_FOUND,
            ErrorCode.DAILY_RECIPE_FORBIDDEN
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "데일리 레시피 등록 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "본인의 AI 레시피가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "AI 레시피를 찾을 수 없음", content = @Content)
    })
    @PostMapping
    public ResponseEntity<DataResponse<DailyRecipeCreateResponseDto>> createDailyRecipe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody DailyRecipeCreateRequestDto request
    ) {
        DailyRecipe dailyRecipe = dailyRecipeService.createDailyRecipe(
                userId,
                request.getAiRecipeId(),
                request.getTitle(),
                request.getDescription(),
                request.getRecipeImageUrl(),
                request.getIsPublic()
        );

        return ResponseEntity.status(201)
                .body(DataResponse.created(DailyRecipeCreateResponseDto.from(dailyRecipe)));
    }

    @Operation(
            summary = "데일리 레시피 상세 조회",
            description = "특정 데일리 레시피의 상세 정보를 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.DAILY_RECIPE_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "데일리 레시피 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "데일리 레시피를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{dailyRecipeId}")
    public ResponseEntity<DataResponse<DailyRecipeDetailResponseDto>> getDailyRecipeDetail(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "데일리 레시피 ID", required = true)
            @PathVariable Long dailyRecipeId
    ) {
        DailyRecipe dailyRecipe = dailyRecipeService.getDailyRecipeDetail(userId, dailyRecipeId);

        return ResponseEntity.ok(DataResponse.from(DailyRecipeDetailResponseDto.from(dailyRecipe)));
    }
}
