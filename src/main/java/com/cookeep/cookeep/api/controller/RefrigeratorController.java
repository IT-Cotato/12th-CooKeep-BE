package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.RefrigeratorIngredientsResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.refrigerator.application.RefrigeratorService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/refrigerator")
public class RefrigeratorController {

    private final RefrigeratorService refrigeratorService;

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
    @GetMapping
    public ResponseEntity<DataResponse<RefrigeratorIngredientsResponseDto>> getAllIngredients(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        RefrigeratorIngredientsResponseDto response = refrigeratorService.getAllIngredients(userId);
        return ResponseEntity.ok(DataResponse.from(response));
    }

}
