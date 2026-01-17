package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientCreateResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.config.JwtTokenProvider;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.UserIngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "(MAIN01) 냉장고 식재료 관리", description = "유저 냉장고 식재료 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/ingredients")
public class UserIngredientController {

    private final UserIngredientService userIngredientService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "식재료 등록", description = "유저가 냉장고에 식재료를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "식재료 등록 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 ENUM 오류"),
            @ApiResponse(responseCode = "404", description = "식재료를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<DataResponse<UserIngredientCreateResponseDto>> createUserIngredient(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid UserIngredientCreateRequestDto request
    ) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getUserId(token, false);

        UserIngredientCreateResponseDto response = userIngredientService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DataResponse.created(response));
    }
}
