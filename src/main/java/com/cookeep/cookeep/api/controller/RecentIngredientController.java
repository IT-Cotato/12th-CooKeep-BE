package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.RecentIngredientsResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.RecentIngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "(MAIN01-3) 최근 추가한 재료", description = "직전 배치에서 추가한 재료 목록 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/ingredients")
public class RecentIngredientController {

    private final RecentIngredientService recentIngredientService;

    @Operation(
            summary = "최근 추가한 재료 목록 조회",
            description = "재료 등록 화면에서 유저가 최근에 추가한 리스트 목록 응답. 첫 등록은 빈 리스트 응답"
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "최근 추가한 재료 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                    서버 오류입니다.
                    - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                    """, content = @Content)
    })
    @GetMapping("/recent")
    public ResponseEntity<DataResponse<RecentIngredientsResponseDto>> getRecentIngredients(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        RecentIngredientsResponseDto response = recentIngredientService.getRecentIngredients(userId);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
