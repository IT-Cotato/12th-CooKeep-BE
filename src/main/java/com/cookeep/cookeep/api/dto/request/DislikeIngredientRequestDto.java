package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "비선호 식재료 수정 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DislikeIngredientRequestDto {

        @NotNull
        private List<@NotNull String> dislikedIngredients;
}
