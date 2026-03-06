package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.request.UserIngredientPreviewRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListCreateResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListPreviewResponseDto;

import java.util.List;

public interface UserIngredientService {

    // 1. 선택한 재료들의 DB 기본값을 조회
    UserIngredientListPreviewResponseDto previewAll(Long userId, List<UserIngredientPreviewRequestDto> requests);

    // 2. 사용자가 확인/수정한 정보로 식재료를 최종 등록
    UserIngredientListCreateResponseDto createAll(Long userId, List<UserIngredientCreateRequestDto> requests);
}
