package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.config.JwtTokenProvider;
import com.cookeep.cookeep.domain.ingredient.customingredient.application.CustomIngredientService;
import com.cookeep.cookeep.domain.ingredient.customingredient.dto.CustomIngredientCreateRequestDto;
import com.cookeep.cookeep.domain.ingredient.customingredient.dto.CustomIngredientCreateResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/ingredients/custom")
public class CustomIngredientController {

    private final CustomIngredientService customIngredientService;
    private final JwtTokenProvider jwtTokenProvider;

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
