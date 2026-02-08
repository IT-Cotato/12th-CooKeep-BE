package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.IngredientListResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.ingredient.common.application.IngredientListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "(MAIN01-2) 재료 등록 페이지에서 식재료 목록 조회", description = "식재료 추가 화면용 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/ingredients")
public class IngredientListController {

    private final IngredientListService ingredientQueryService;

    @Operation(
            summary = "카테고리별 모든 식재료 조회",
            description = "식재료 추가 화면에서 사용할 카테고리별 모든 식재료 목록을 조회합니다. " +
                    "모든 식재료는 아이디 오름차순 정렬. 디폴트 재료 뒤에 유저 개인 커스텀 식재료 정렬."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "식재료 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                    서버 오류입니다.
                    - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                    """, content = @Content)
    })
    @GetMapping("/list")
    public ResponseEntity<DataResponse<IngredientListResponseDto>> getAllIngredients(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        IngredientListResponseDto response = ingredientQueryService.getAllIngredientsByCategory(userId);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
