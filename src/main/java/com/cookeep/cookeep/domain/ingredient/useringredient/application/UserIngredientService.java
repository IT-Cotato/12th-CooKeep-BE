package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListCreateResponseDto;

import java.util.List;

public interface UserIngredientService {

    UserIngredientListCreateResponseDto createAll(Long userId, List<UserIngredientCreateRequestDto> requests);
}
