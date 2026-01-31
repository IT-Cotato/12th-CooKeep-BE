package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.security.JwtTokenProvider;
import com.cookeep.cookeep.domain.ingredient.customingredient.application.CustomIngredientService;
import com.cookeep.cookeep.api.dto.request.CustomIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.CustomIngredientCreateResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag( name = "(MAIN01) 커스텀 재료 등록", description = "커스텀 식재료 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/ingredients/custom")
public class CustomIngredientController {

    private final CustomIngredientService customIngredientService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation( summary = "커스텀 식재료 등록", description = "유저가 새로운 커스텀 식재료를 등록합니다."
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.CUSTOM_INGREDIENT_REQUIRED_FIELDS_MISSING,
            ErrorCode.INVALID_STORAGE_TYPE,
            ErrorCode.INVALID_CATEGORY_TYPE,
            ErrorCode.DUPLICATE_CUSTOM_INGREDIENT,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "커스텀 식재료 등록 성공"),
            @ApiResponse(responseCode = "400", description = """
                잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                - CUSTOM_INGREDIENT_REQUIRED_FIELDS_MISSING: 필수 필드가 누락되었습니다.
                - INVALID_STORAGE_TYPE: storage ENUM 값이 올바르지 않습니다.
                - INVALID_CATEGORY_TYPE: category ENUM 값이 올바르지 않습니다.
                """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                인증 실패입니다. 다음 오류가 발생할 수 있습니다:
                - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                """, content = @Content),
            @ApiResponse(responseCode = "409", description = """
                충돌이 발생했습니다. 다음 오류가 발생할 수 있습니다:
                - DUPLICATE_CUSTOM_INGREDIENT: 동일 이름의 커스텀 식재료가 이미 존재합니다.
                """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                서버 오류입니다. 다음 오류가 발생할 수 있습니다:
                - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                """, content = @Content)
    })
    @PostMapping
    public ResponseEntity<DataResponse<CustomIngredientCreateResponseDto>> createCustomIngredient(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid CustomIngredientCreateRequestDto request
    ) {
        String token = authorization.replace("Bearer ", "");

        // JwtTokenProvider로 userId 추출
        Long userId = jwtTokenProvider.getUserId(token, false);

        CustomIngredientCreateResponseDto response =
                customIngredientService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DataResponse.created(response));
    }
}
