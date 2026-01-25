package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.domain.recipe.application.AiRecipeService;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeAdoptResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeRequestDto;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/ai/recipes")
@RequiredArgsConstructor
public class AiRecipeController {

    private final AiRecipeService aiRecipeService;

    // AI에 레시피 요청/변경
    @PostMapping
    public ResponseEntity<AiRecipeResponseDto> generateRecipe(
            @Valid @RequestBody AiRecipeRequestDto request
    ) {
        // 테스트용 userId 하드코딩
        Long testUserId = 1L;

        AiRecipeResponseDto response =
                aiRecipeService.generateRecipe(testUserId, request);

        return ResponseEntity.ok(response);
    }

    // 레시피 채택
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<AiRecipeAdoptResponseDto> adoptRecipe(
            @PathVariable Long sessionId
    ) {
        //  ❗️테스트용 userId 하드코딩
        Long testUserId = 1L;

        AiRecipeAdoptResponseDto response =
                aiRecipeService.adoptRecipe(testUserId, sessionId);

        return ResponseEntity.ok(response);
    }
}
