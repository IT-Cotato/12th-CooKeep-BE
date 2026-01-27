package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.recipe.application.AiRecipeService;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeAdoptResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeRequestDto;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "(MAIN05) 레시피검색",
        description = "AI를 이용한 레시피 생성 및 채택 API"
)
@RestController
@RequestMapping("/api/users/me/ai/recipes")
@RequiredArgsConstructor
public class AiRecipeController {

    private final AiRecipeService aiRecipeService;

    @Operation(
            summary = "AI 레시피 생성 / 변경",
            description = "유저의 식재료 및 조건을 기반으로 AI에게 레시피를 요청하거나 기존 요청을 변경합니다."
    )
    @SecurityRequirements
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 레시피 생성 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - INVALID_MESSAGE_TYPE: 허용되지 않은 메시지 타입입니다.
                    - RECIPE_INGREDIENTS_REQUIRED: 레시피 생성을 위한 재료가 필요합니다.
                    - INVALID_DIFFICULTY: 유효하지 않은 난이도입니다.
                    - AI_RECIPE_CHANGE_LIMIT_EXCEEDED: 레시피 변경 횟수를 초과했습니다.
                    - INVALID_INGREDIENT_TYPE: 유효하지 않은 재료 타입입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                    - AI_SESSION_NOT_FOUND: AI 레시피 세션을 찾을 수 없습니다.
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
            @Valid @RequestBody AiRecipeRequestDto request
    ) {
        // 테스트용 userId 하드코딩
        Long testUserId = 1L;

        AiRecipeResponseDto response =
                aiRecipeService.generateRecipe(testUserId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "AI 레시피 채택",
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
            @PathVariable Long sessionId
    ) {
        //  ❗️테스트용 userId 하드코딩
        Long testUserId = 1L;

        AiRecipeAdoptResponseDto response =
                aiRecipeService.adoptRecipe(testUserId, sessionId);

        return ResponseEntity.ok(response);
    }
}
