package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.AiRecipeRetryDto;
import com.cookeep.cookeep.api.dto.response.AiSessionDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.AiSessionListResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.recipe.application.AiRecipeService;
import com.cookeep.cookeep.api.dto.response.AiRecipeAdoptResponseDto;
import com.cookeep.cookeep.api.dto.request.AiRecipeRequestDto;
import com.cookeep.cookeep.api.dto.response.AiRecipeResponseDto;
import com.cookeep.cookeep.security.JwtTokenProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "(MAIN05, 06) AI 레시피",
        description = "AI를 이용한 레시피 생성/채택/조회 API"
)
@RestController
@RequestMapping("/api/users/me/ai/recipes")
@RequiredArgsConstructor
public class AiRecipeController {

    private final AiRecipeService aiRecipeService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
            summary = "(MAIN05-01)AI 레시피 생성",
            description = "유저의 식재료 및 조건을 기반으로 AI에게 레시피를 요청합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.RECIPE_INGREDIENTS_REQUIRED,
            ErrorCode.INVALID_DIFFICULTY,
            ErrorCode.INVALID_INGREDIENT_TYPE,
            ErrorCode.INGREDIENT_NOT_FOUND,
            ErrorCode.AI_SEARCH_FAILED,
            ErrorCode.AI_RESPONSE_PARSE_FAILED,
            ErrorCode.AI_RESPONSE_INVALID_FORMAT,
            ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 레시피 생성 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - RECIPE_INGREDIENTS_REQUIRED: 레시피 생성을 위한 재료가 필요합니다.
                    - INVALID_DIFFICULTY: 유효하지 않은 난이도입니다.
                    - INVALID_INGREDIENT_TYPE: 유효하지 않은 재료 타입입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                    - INGREDIENT_NOT_FOUND: 유저가 보유한 재료를 찾을 수 없습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                    서버 오류입니다. 다음 오류가 발생할 수 있습니다:
                    - AI_SEARCH_FAILED: AI 요청 또는 저장 처리에 실패했습니다.
                    - AI_RESPONSE_PARSE_FAILED: AI 응답 파싱에 실패했습니다.
                    - AI_RESPONSE_INVALID_FORMAT: AI 응답 형식이 올바르지 않습니다.
                    - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                    """, content = @Content)
    })
    @PostMapping
    public ResponseEntity<AiRecipeResponseDto> generateRecipe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody AiRecipeRequestDto request
    ) {

        AiRecipeResponseDto response =
                aiRecipeService.generateRecipe(userId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "(MAIN05-02)AI 레시피 재요청",
            description = "기존 세션의 식재료 및 조건을 기반으로 AI에게 레시피를 재요청합니다."
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.RECIPE_SESSIONID_REQUIRED,
            ErrorCode.AI_SESSION_NOT_FOUND,
            ErrorCode.SESSION_ALREADY_COMPLETED,
            ErrorCode.AI_RECIPE_CHANGE_LIMIT_EXCEEDED,
            ErrorCode.SESSION_DIFFICULTY_NOT_FOUND,
            ErrorCode.SESSION_INGREDIENTS_NOT_FOUND,
            ErrorCode.RECIPE_TITLE_PARSE_FAILED,
            ErrorCode.AI_SEARCH_FAILED,
            ErrorCode.AI_RECIPE_TITLE_MISSING,
            ErrorCode.AI_RESPONSE_PARSE_FAILED,
            ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 레시피 재생성 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - RECIPE_SESSIONID_REQUIRED: 레시피 요청에 필요한 값이 누락되었습니다.
                    - SESSION_ALREADY_COMPLETED: 이미 완료된 세션입니다.
                    - AI_RECIPE_CHANGE_LIMIT_EXCEEDED: 레시피 변경 횟수를 초과했습니다.
                    - SESSION_DIFFICULTY_NOT_FOUND: 세션의 난이도 정보를 찾을 수 없습니다.
                    - SESSION_INGREDIENTS_NOT_FOUND: 세션의 재료 정보를 찾을 수 없습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                    - AI_SESSION_NOT_FOUND: AI 레시피 세션을 찾을 수 없습니다.
                    - AI_RECIPE_TITLE_MISSING: AI 응답에 레시피 제목이 존재하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                    서버 오류입니다. 다음 오류가 발생할 수 있습니다:
                    - AI_SEARCH_FAILED: AI 요청 또는 저장 처리에 실패했습니다.
                    - AI_RESPONSE_PARSE_FAILED: AI 응답 파싱에 실패했습니다.
                    - RECIPE_TITLE_PARSE_FAILED: 레시피 제목 파싱에 실패했습니다.
                    - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                    """, content = @Content)
    })
    @PostMapping("/retry")
    public ResponseEntity<AiRecipeResponseDto> regenerateRecipe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody AiRecipeRetryDto request
    ) {
        return ResponseEntity.ok(
                aiRecipeService.regenerateRecipe(userId, request.getSessionId())
        );
    }

    @Operation(
            summary = "(MAIN05-03)AI 레시피 채택",
            description = "생성된 AI 레시피 중 하나를 최종 레시피로 채택하고 세션을 완료 처리합니다."
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.AI_SESSION_NOT_FOUND,
            ErrorCode.SESSION_ALREADY_COMPLETED,
            ErrorCode.AI_RESPONSE_INVALID_FORMAT,
            ErrorCode.AI_RESPONSE_PARSE_FAILED,
            ErrorCode.AI_SEARCH_FAILED,
            ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 레시피 채택 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - SESSION_ALREADY_COMPLETED: 이미 완료된 세션입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                    - AI_SESSION_NOT_FOUND: AI 레시피 세션을 찾을 수 없습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                    서버 오류입니다. 다음 오류가 발생할 수 있습니다:
                    - AI_SEARCH_FAILED: 레시피 저장 또는 처리에 실패했습니다.
                    - AI_RESPONSE_PARSE_FAILED: AI 응답 파싱에 실패했습니다.
                    - AI_RESPONSE_INVALID_FORMAT: AI 응답 형식이 올바르지 않습니다.
                    - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                    """, content = @Content)
    })
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<AiRecipeAdoptResponseDto> adoptRecipe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long sessionId
    ) {

        AiRecipeAdoptResponseDto response =
                aiRecipeService.adoptRecipe(userId, sessionId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "(MAIN06-1) AI 레시피 대화 세션 목록 조회",
            description = "사용자의 모든 AI 레시피 대화 세션 조회 (즐겨찾기 분리, 최신순 정렬)"
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED
    })
    @GetMapping("/sessions")
    public ResponseEntity<DataResponse<AiSessionListResponseDto>> getAllSessions(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {

        AiSessionListResponseDto response = aiRecipeService.getAllSessions(userId);

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "(MAIN06-2)AI 레시피 대화 세션 상세 조회",
            description = "특정 세션의 모든 대화 내역 조회 (AI 응답만)"
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.AI_SESSION_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<DataResponse<AiSessionDetailResponseDto>> getSessionDetail(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId
    ) {

        AiSessionDetailResponseDto response = aiRecipeService.getSessionDetail(userId, sessionId);

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "(MAIN06-3)AI 레시피 대화 세션 삭제",
            description = "특정 세션과 관련 메시지 모두 삭제"
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.AI_SESSION_NOT_FOUND,
            ErrorCode.AI_SESSION_FORBIDDEN
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "본인의 대화 세션이 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<DataResponse<Void>> deleteSession(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId
    ) {

        aiRecipeService.deleteSession(userId, sessionId);

        return ResponseEntity.ok(DataResponse.ok());
    }

    @Operation(
            summary = "(MAIN07) AI 대화 세션 즐겨찾기 추가/삭제",
            description = "특정 세션의 즐겨찾기 상태를 변경합니다. (T -> F / F -> T)"
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.AI_SESSION_NOT_FOUND,
            ErrorCode.AI_SESSION_FORBIDDEN
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "즐겨찾기 상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "본인의 대화 세션이 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음", content = @Content)
    })
    @PatchMapping("/sessions/{sessionId}")
    public ResponseEntity<DataResponse<Void>> toggleFavorite(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "세션 ID", required = true)
            @PathVariable Long sessionId
    ) {

        aiRecipeService.toggleFavorite(userId, sessionId);

        return ResponseEntity.ok(DataResponse.ok());
    }
}
