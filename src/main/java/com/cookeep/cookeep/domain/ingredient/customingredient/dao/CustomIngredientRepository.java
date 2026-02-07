package com.cookeep.cookeep.domain.ingredient.customingredient.dao;

import com.cookeep.cookeep.domain.ingredient.common.Category;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomIngredientRepository extends JpaRepository<CustomIngredient, Long> {
    boolean existsByUserIdAndName(Long userId, String name);
    Optional<CustomIngredient> findByIdAndUserId(Long id, Long userId);

    // 유저 별 커스텀 식재료를 카테고리 별 조회 (ID 오름차순)
    List<CustomIngredient> findByUserIdAndCategoryOrderByIdAsc(Long userId, Category category);

    // 특정 유저의 모든 커스텀 식재료 조회 (ID 오름차순)
    List<CustomIngredient> findByUserIdOrderByIdAsc(Long userId);
}
