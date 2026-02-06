package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.PaginatedIngredientsResponseDto;
import com.cookeep.cookeep.api.dto.response.RefrigeratorIngredientsResponseDto;
import com.cookeep.cookeep.api.dto.response.RefrigeratorSearchResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientDetailResponseDto;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "(MAIN03) 냉장고 관리", description = "냉장고 식재료 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/refrigerator")
public class RefrigeratorController {

    private final RefrigeratorService refrigeratorService;

    @Operation(
            summary = "01 냉장고 전체 식재료 조회",
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
            summary = "02냉장고 식재료 전체보기 조회",
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

    @Operation(
            summary = "03 재료 상세 조회",
            description = "특정 식재료의 상세 정보를 조회합니다."
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INGREDIENT_NOT_FOUND
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                    - USER_INGREDIENT_NOT_FOUND: 해당 식재료를 찾을 수 없습니다.
                    """, content = @Content)
    })
    @GetMapping("/{ingredientId}")
    public ResponseEntity<DataResponse<UserIngredientDetailResponseDto>> getIngredientDetail(
            @AuthenticationPrincipal(expression = "userId") Long userId,

            @Parameter(description = "유저 식재료 ID", required = true)
            @PathVariable Long ingredientId
    ) {
        UserIngredientDetailResponseDto response = refrigeratorService.getIngredientDetail(userId, ingredientId);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "04 식재료 검색",
            description = "재료명으로 검색하고, 보관 장소 및 정렬 옵션을 적용하여 조회합니다."
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.REFRIGERATOR_INVALID_QUERY
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - REFRIGERATOR_INVALID_QUERY: 잘못된 쿼리 파라미터입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content)
    })
    @GetMapping("/search")
    public ResponseEntity<DataResponse<RefrigeratorSearchResponseDto>> searchIngredients(
            @AuthenticationPrincipal(expression = "userId") Long userId,

            @Parameter(description = "검색어 (재료명 부분 검색)")
            @RequestParam(required = false) String q,

            @Parameter(
                    description = "보관 장소 필터",
                    schema = @Schema(allowableValues = {"FRIDGE", "FREEZER", "PANTRY"})
            )
            @RequestParam(required = false) Storage storage,

            @Parameter(
                    description = "정렬 방식 (기본값: EXPIRATION_ASC)",
                    schema = @Schema(allowableValues = {"EXPIRATION_ASC", "CREATED_DESC", "CREATED_ASC"})
            )
            @RequestParam(required = false, defaultValue = "EXPIRATION_ASC") IngredientSort sort,

            @Parameter(description = "페이지 번호 (기본값: 0)")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "페이지 크기 (기본값: 20)")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        RefrigeratorSearchResponseDto response = refrigeratorService.searchIngredients(
                userId, q, storage, sort, page, size
        );
        return ResponseEntity.ok(DataResponse.from(response));
    }

}
