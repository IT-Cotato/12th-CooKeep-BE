package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.DailyRecipeCreateRequestDto;
import com.cookeep.cookeep.api.dto.request.DailyRecipeUpdateRequestDto;
import com.cookeep.cookeep.api.dto.request.DailyRecipeVisibilityRequestDto;
import com.cookeep.cookeep.api.dto.response.AdoptedAiRecipeDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.AdoptedAiRecipeListResponseDto;
import com.cookeep.cookeep.api.dto.response.DailyRecipeCreateResponseDto;
import com.cookeep.cookeep.api.dto.response.DailyRecipeDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.DailyRecipeUpdateResponseDto;
import com.cookeep.cookeep.api.dto.response.DailyRecipeCalendarResponseDto;
import com.cookeep.cookeep.api.dto.response.DailyRecipeListResponseDto;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            description = "데일리 레시피 등록을 위해 유저가 채택한 AI 레시피 목록을 조회합니다."
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
            description = "채택된 AI 레시피를 기반으로 데일리 요리를 기록합니다." +
                    "title은 미입력 시 AI 레시피 기본 제목을 사용합니다." +
                    "요리 사진 URL은 이미지 업로드 API로 먼저 업로드 후 URL을 전달하는 방식입니다."
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
        DailyRecipeService.DailyRecipeResult result = dailyRecipeService.createDailyRecipe(
                userId,
                request.getAiRecipeId(),
                request.getTitle(),
                request.getDescription(),
                request.getRecipeImageUrl(),
                request.getCroppedImageUrl(),
                request.getIsPublic()
        );

        return ResponseEntity.status(201)
                .body(DataResponse.created(DailyRecipeCreateResponseDto.from(result.dailyRecipe(), result.reward())));
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

    @Operation(
            summary = "데일리 레시피 수정",
            description = "데일리 레시피의 제목, 한줄평, 요리 사진을 수정합니다. " +
                    "요리 사진은 이미지 업로드 API로 먼저 업로드 후 URL을 전달하는 방식입니다. " +
                    "기존에 사진이 없었던 레시피에 사진을 새로 추가하면 쿠키가 지급됩니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.DAILY_RECIPE_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "데일리 레시피 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "데일리 레시피를 찾을 수 없음", content = @Content)
    })
    @PatchMapping("/{dailyRecipeId}")
    public ResponseEntity<DataResponse<DailyRecipeUpdateResponseDto>> updateDailyRecipe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "데일리 레시피 ID", required = true)
            @PathVariable Long dailyRecipeId,
            @Valid @RequestBody DailyRecipeUpdateRequestDto request
    ) {
        DailyRecipeService.DailyRecipeResult result = dailyRecipeService.updateDailyRecipe(
                userId, dailyRecipeId, request.getTitle(), request.getDescription(),
                request.getRecipeImageUrl(), request.getCroppedImageUrl(), request.getDeleteRecipeImage()
        );

        return ResponseEntity.ok(DataResponse.from(DailyRecipeUpdateResponseDto.from(result.dailyRecipe(), result.reward())));
    }

    @Operation(
            summary = "날짜 기반 데일리 레시피 목록 조회",
            description = "특정 날짜에 등록된 데일리 레시피 목록을 조회합니다. " +
                    "마이쿠킵 화면 진입 시(오늘 날짜) 또는 캘린더 날짜 클릭 시 사용됩니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "날짜별 데일리 레시피 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping
    public ResponseEntity<DataResponse<List<DailyRecipeListResponseDto>>> getDailyRecipesByDate(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", required = true, example = "2026-01-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<DailyRecipe> dailyRecipes = dailyRecipeService.getDailyRecipesByDate(userId, date);
        List<DailyRecipeListResponseDto> response = dailyRecipes.stream()
                .map(DailyRecipeListResponseDto::from)
                .toList();

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "캘린더 마킹용 데일리 레시피 리스트 조회",
            description = "특정 연/월에 요리 기록이 있는 날짜 목록과 각 날짜의 첫 번째 레시피 이미지를 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "캘린더 마킹 데이터 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/calendar")
    public ResponseEntity<DataResponse<List<DailyRecipeCalendarResponseDto>>> getCalendarMarking(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "조회 연도", required = true, example = "2025")
            @RequestParam int year,
            @Parameter(description = "조회 월 (1-12)", required = true, example = "12")
            @RequestParam int month
    ) {
        List<DailyRecipeCalendarResponseDto> response = dailyRecipeService.getCalendarMarking(userId, year, month);

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "데일리 레시피 삭제",
            description = "데일리 레시피를 삭제합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.DAILY_RECIPE_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "데일리 레시피 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "데일리 레시피를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{dailyRecipeId}")
    public ResponseEntity<DataResponse<Void>> deleteDailyRecipe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "데일리 레시피 ID", required = true)
            @PathVariable Long dailyRecipeId
    ) {
        dailyRecipeService.deleteDailyRecipe(userId, dailyRecipeId);

        return ResponseEntity.ok(DataResponse.ok());
    }

    @Operation(
            summary = "데일리 레시피 공개 범위 수정",
            description = "데일리 레시피의 공개/비공개 상태를 변경합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.DAILY_RECIPE_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공개 범위 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "데일리 레시피를 찾을 수 없음", content = @Content)
    })
    @PatchMapping("/{dailyRecipeId}/visibility")
    public ResponseEntity<DataResponse<Void>> updateDailyRecipeVisibility(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "데일리 레시피 ID", required = true)
            @PathVariable Long dailyRecipeId,
            @Valid @RequestBody DailyRecipeVisibilityRequestDto request
    ) {
        DailyRecipe dailyRecipe = dailyRecipeService.updateDailyRecipeVisibility(
                userId, dailyRecipeId, request.getIsPublic()
        );

        return ResponseEntity.ok(DataResponse.ok());
    }
}
