package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.domain.recipe.application.AiRecipeService;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeRequestDto;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/ai/recipes")
@RequiredArgsConstructor
public class AiRecipeController {

    private final AiRecipeService aiRecipeService;

    /**
     * AI 레시피 생성 / 변경
     */
    @PostMapping
    public ResponseEntity<AiRecipeResponseDto> generateRecipe(
            @Valid @RequestBody AiRecipeRequestDto request
    ) {
        // 🔥 지금은 인증 붙이기 전이니까 테스트용 userId 하드코딩
        Long testUserId = 1L;

        AiRecipeResponseDto response =
                aiRecipeService.generateRecipe(testUserId, request);

        return ResponseEntity.ok(response);
    }
}
