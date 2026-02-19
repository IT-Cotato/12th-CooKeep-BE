package com.cookeep.cookeep.domain.ingredient.common.application;

import com.cookeep.cookeep.api.dto.response.IngredientListResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.domain.Category;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientListService {

    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;

    public IngredientListResponseDto getAllIngredientsByCategory(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 모든 카테고리 순회
        List<IngredientListResponseDto.CategoryGroup> categoryGroups =
                Arrays.stream(Category.values())
                        .map(category -> buildCategoryGroup(category, userId))
                        .filter(group -> !group.getIngredients().isEmpty()) // 에러 방지용 빈 카테고리 제외
                        .collect(Collectors.toList());

        return IngredientListResponseDto.builder()
                .categories(categoryGroups)
                .build();
    }

    private IngredientListResponseDto.CategoryGroup buildCategoryGroup(Category category, Long userId) {
        // 1. 기본 식재료 조회 (ID 오름차순)
        List<DefaultIngredient> defaultIngredients =
                defaultIngredientRepository.findByCategoryOrderByIdAsc(category);

        // 2. 커스텀 식재료 조회 (ID 오름차순)
        List<CustomIngredient> customIngredients =
                customIngredientRepository.findByUserIdAndCategoryOrderByIdAsc(userId, category);

        // 3. DTO 변환 및 병합
        List<IngredientListResponseDto.IngredientItem> items = new ArrayList<>();

        // 기본 식재료 추가 후 뒤에 커스텀 식재료 추가
        defaultIngredients.forEach(ingredient ->
                items.add(IngredientListResponseDto.IngredientItem.builder()
                        .id(ingredient.getId())
                        .type("DEFAULT")
                        .name(ingredient.getIngredient())
                        .imageUrl(ingredient.getImageUrl())
                        .category(ingredient.getCategory())
                        .unit(ingredient.getUnit() != null ? ingredient.getUnit().name() : Unit.PIECE.name())
                        .build())
        );
        customIngredients.forEach(ingredient ->
                items.add(IngredientListResponseDto.IngredientItem.builder()
                        .id(ingredient.getId())
                        .type("CUSTOM")
                        .name(ingredient.getName())
                        .imageUrl(ingredient.getImageUrl())
                        .category(ingredient.getCategory())
                        .unit(Unit.PIECE.name())
                        .build())
        );

        return IngredientListResponseDto.CategoryGroup.builder()
                .category(category)
                .displayName(category.getDisplayName())
                .ingredients(items)
                .build();
    }

}
