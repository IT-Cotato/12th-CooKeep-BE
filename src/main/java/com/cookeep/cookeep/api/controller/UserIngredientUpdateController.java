package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.UpdateExpirationRequestDto;
import com.cookeep.cookeep.api.dto.request.UpdateMemoRequestDto;
import com.cookeep.cookeep.api.dto.request.UpdateQuantityRequestDto;
import com.cookeep.cookeep.api.dto.request.UpdateStorageRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientDetailResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.UserIngredientUpdateService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/ingredients")
@RequiredArgsConstructor
public class UserIngredientUpdateController {

    private final UserIngredientUpdateService userIngredientUpdateService;

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
