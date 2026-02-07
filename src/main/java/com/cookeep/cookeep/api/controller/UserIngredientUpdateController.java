package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.UpdateExpirationRequestDto;
import com.cookeep.cookeep.api.dto.request.UpdateMemoRequestDto;
import com.cookeep.cookeep.api.dto.request.UpdateQuantityRequestDto;
import com.cookeep.cookeep.api.dto.request.UpdateStorageRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientDetailResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.UserIngredientUpdateService;
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

@Tag(name = "식재료 상태 변경", description = "식재료 상태변경 API")
@RestController
@RequestMapping("/api/users/me/ingredients")
@RequiredArgsConstructor
public class UserIngredientUpdateController {

    private final UserIngredientUpdateService userIngredientUpdateService;

    @Operation(
            summary = "1. 보관 장소 변경",
            description = "식재료의 보관 장소를 변경합니다 (FRIDGE/FREEZER/PANTRY)"
    )
    @ApiErrorCodeExamples({
            ErrorCode.INVALID_STORAGE_TYPE,
            ErrorCode.INGREDIENT_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "보관 장소 변경 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - INVALID_STORAGE_TYPE: 잘못된 보관 타입입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    재료를 찾을 수 없습니다.
                    - INGREDIENT_NOT_FOUND: 해당 식재료가 존재하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content)
    })
    @PatchMapping("/{ingredientId}/storage")
    public ResponseEntity<DataResponse<UserIngredientDetailResponseDto>> updateStorage(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "식재료 ID", required = true)
            @PathVariable Long ingredientId,
            @Valid @RequestBody UpdateStorageRequestDto request
    ) {
        UserIngredientDetailResponseDto response = userIngredientUpdateService.updateStorage(
                userId, ingredientId, request
        );
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "2. 유통기한 변경",
            description = "식재료의 유통기한을 변경합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.INVALID_PARAMETER,
            ErrorCode.INGREDIENT_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유통기한 변경 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - INVALID_PARAMETER: 날짜 형식이 잘못되었습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    재료를 찾을 수 없습니다.
                    - INGREDIENT_NOT_FOUND: 해당 식재료가 존재하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content)
    })
    @PatchMapping("/{ingredientId}/date")
    public ResponseEntity<DataResponse<UserIngredientDetailResponseDto>> updateExpirationDate(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "식재료 ID", required = true)
            @PathVariable Long ingredientId,
            @Valid @RequestBody UpdateExpirationRequestDto request
    ) {
        UserIngredientDetailResponseDto response = userIngredientUpdateService.updateExpirationDate(
                userId, ingredientId, request
        );
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "3. 수량 변경",
            description = "식재료의 수량을 변경합니다 (1 이상)"
    )
    @ApiErrorCodeExamples({
            ErrorCode.INVALID_QUANTITY,
            ErrorCode.INGREDIENT_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - INVALID_QUANTITY: 수량은 1 이상이어야 합니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    재료를 찾을 수 없습니다.
                    - INGREDIENT_NOT_FOUND: 해당 식재료가 존재하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content)
    })
    @PatchMapping("/{ingredientId}/quantity")
    public ResponseEntity<DataResponse<UserIngredientDetailResponseDto>> updateQuantity(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "식재료 ID", required = true)
            @PathVariable Long ingredientId,
            @Valid @RequestBody UpdateQuantityRequestDto request
    ) {
        UserIngredientDetailResponseDto response = userIngredientUpdateService.updateQuantity(
                userId, ingredientId, request
        );
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "4. 메모 변경",
            description = "식재료의 메모를 변경합니다 (빈 문자열은 메모 삭제, 100자 제한, 이모티콘은 두 글자로 계산)"
    )
    @ApiErrorCodeExamples({
            ErrorCode.MEMO_TOO_LONG,
            ErrorCode.INGREDIENT_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메모 변경 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - MEMO_TOO_LONG: 메모 길이는 최대 100자 입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    재료를 찾을 수 없습니다.
                    - INGREDIENT_NOT_FOUND: 해당 식재료가 존재하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content)
    })
    @PatchMapping("/{ingredientId}/memo")
    public ResponseEntity<DataResponse<UserIngredientDetailResponseDto>> updateMemo(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "식재료 ID", required = true)
            @PathVariable Long ingredientId,
            @Valid @RequestBody UpdateMemoRequestDto request
    ) {
        UserIngredientDetailResponseDto response = userIngredientUpdateService.updateMemo(
                userId, ingredientId, request
        );
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
