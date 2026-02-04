package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientCreateResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.config.JwtTokenProvider;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.UserIngredientService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "(MAIN01) 냉장고 식재료 관리", description = "유저 냉장고 식재료 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/ingredients")
public class UserIngredientController {

    private final UserIngredientService userIngredientService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
            summary = "유저 식재료 등록",
            description = "유저가 보유 중인 식재료를 등록합니다. (기본 식재료 또는 커스텀 식재료)"
    )
    @SecurityRequirements
    @ApiErrorCodeExamples({
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "식재료 등록 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다. 다음 오류가 발생할 수 있습니다:
                    - (Validation/Jackson) 필수값 누락 또는 ENUM 값 오류
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                    - USER_NOT_FOUND: 유저를 찾을 수 없습니다.
                    - INGREDIENT_REFERENCE_NOT_FOUND: 참조한 식재료(기본/커스텀)를 찾을 수 없습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                    서버 오류입니다. 다음 오류가 발생할 수 있습니다:
                    - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                    """, content = @Content)
    })
    @PostMapping
    public ResponseEntity<DataResponse<UserIngredientCreateResponseDto>> createUserIngredient(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestBody @Valid UserIngredientCreateRequestDto request
    ) {
        UserIngredientCreateResponseDto response = userIngredientService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DataResponse.created(response));
    }
}
