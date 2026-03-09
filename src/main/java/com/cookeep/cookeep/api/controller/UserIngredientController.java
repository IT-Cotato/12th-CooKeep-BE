package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.UserIngredientListCreateRequestDto;
import com.cookeep.cookeep.api.dto.request.UserIngredientListPreviewRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListCreateResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListPreviewResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.UserIngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "(MAIN01) 냉장고 식재료 관리", description = "유저 냉장고 식재료 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/ingredients")
public class UserIngredientController {

    private final UserIngredientService userIngredientService;

    // 1단계: 기본 정보 조회
    @Operation(
            summary = "[1단계] 식재료 기본 정보 조회",
            description = "재료 DB의 기본 정보(보관장소, 유통기한, 단위 등) 조회"
    )
    @ApiErrorCodeExamples({
            ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.INVALID_INGREDIENT_REQUEST
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기본 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다.
                    - INVALID_INGREDIENT_REQUEST: 필수값(type, referenceId) 누락 또는 ENUM 값 오류
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    - INGREDIENT_REFERENCE_NOT_FOUND: 참조한 식재료를 찾을 수 없습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    @PostMapping("/preview")
    public ResponseEntity<DataResponse<UserIngredientListPreviewResponseDto>> previewUserIngredients(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestBody @Valid UserIngredientListPreviewRequestDto request
    ) {
        UserIngredientListPreviewResponseDto response =
                userIngredientService.previewAll(userId, request.getIngredients());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(DataResponse.from(response));
    }

    // 2단계: 최종 등록
    @Operation(
            summary = "[2단계] 유저 식재료 등록",
            description = """
                    유저 확인/수정한 정보로 식재료 최종 등록
                    - type, referenceId는 필수입니다.
                    - 나머지 필드(quantity, unit, storage, expirationDate, memo)는 선택입니다.
                    - 미입력 시 DB 기본값 또는 시스템 기본값이 적용
                    """
    )
    @ApiErrorCodeExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR,
            ErrorCode.INVALID_INGREDIENT_REQUEST
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "식재료 등록 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다.
                    - INVALID_INGREDIENT_REQUEST: 필수값 누락 또는 ENUM 값 오류
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    - USER_NOT_FOUND: 유저를 찾을 수 없습니다.
                    - INGREDIENT_REFERENCE_NOT_FOUND: 참조한 식재료를 찾을 수 없습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content)
    })
    @PostMapping
    public ResponseEntity<DataResponse<UserIngredientListCreateResponseDto>> createUserIngredients(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestBody @Valid UserIngredientListCreateRequestDto request
    ) {
        UserIngredientListCreateResponseDto response = userIngredientService.createAll(userId, request.getIngredients());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DataResponse.created(response));
    }
}
