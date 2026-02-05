package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.PaginatedIngredientsResponseDto;
import com.cookeep.cookeep.api.dto.response.RefrigeratorIngredientsResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.ingredient.common.Storage;
import com.cookeep.cookeep.domain.refrigerator.application.RefrigeratorService;
import com.cookeep.cookeep.domain.refrigerator.entity.IngredientSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "(MAIN02) 냉장고 관리", description = "냉장고 식재료 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/refrigerator")
public class RefrigeratorController {

    private final RefrigeratorService refrigeratorService;

    @Operation(
            summary = "냉장고 전체 식재료 조회",
            description = "냉장/냉동/상온 구분하여 모든 식재료를 조회합니다. 각 보관 장소 내에서 유통기한 임박순(leftDays 오름차순)으로 정렬됩니다."
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content)
    })
    @GetMapping("/home")
    public ResponseEntity<DataResponse<RefrigeratorIngredientsResponseDto>> getAllIngredients(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        RefrigeratorIngredientsResponseDto response = refrigeratorService.getAllIngredients(userId);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "냉장고 식재료 전체보기 조회",
            description = "냉장/냉동/상온 위치별 식재료를 정렬 옵션 적용하여 조회합니다."
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.REFRIGERATOR_INVALID_QUERY,
            ErrorCode.INVALID_STORAGE_TYPE,
            ErrorCode.INVALID_SORT_TYPE
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - INVALID_QUERY: 잘못된 쿼리 파라미터입니다.
                    - INVALID_STORAGE_TYPE: 유효하지 않은 보관 장소 타입입니다.
                    - INVALID_SORT_TYPE: 유효하지 않은 정렬 타입입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content)
    })
    @GetMapping()
    public ResponseEntity<DataResponse<PaginatedIngredientsResponseDto>> getIngredientsByStorage(
            @AuthenticationPrincipal(expression = "userId") Long userId,

            @Parameter(
                    description = "보관 장소",
                    required = true,
                    schema = @Schema(allowableValues = {"FRIDGE", "FREEZER", "PANTRY"})
            )
            @RequestParam Storage storage,

            @Parameter(
                    description = "정렬 기준 (기본값: EXPIRATION_ASC)",
                    schema = @Schema(allowableValues = {"EXPIRATION_ASC", "CREATED_DESC", "CREATED_ASC"})
            )
            @RequestParam(required = false, defaultValue = "EXPIRATION_ASC") IngredientSort sort,

            @Parameter(description = "페이지 번호 (상하스크롤)")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "페이지 크기")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        PaginatedIngredientsResponseDto response = refrigeratorService.getIngredientsByStorage(
                userId, storage, sort, page, size
        );
        return ResponseEntity.ok(DataResponse.from(response));
    }

}
