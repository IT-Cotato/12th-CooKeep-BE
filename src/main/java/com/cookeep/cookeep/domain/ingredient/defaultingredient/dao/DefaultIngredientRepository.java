package com.cookeep.cookeep.domain.ingredient.defaultingredient.dao;

import com.cookeep.cookeep.domain.ingredient.common.domain.Category;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefaultIngredientRepository extends JpaRepository<DefaultIngredient, Long> {

    // 카테고리 별 기본 식재료 조회 (ID 오름차순으로 정렬)
    List<DefaultIngredient> findByCategoryOrderByIdAsc(Category category);

    // 모든 기본 식재료 조회 (ID 오름차순)
    List<DefaultIngredient> findAllByOrderByIdAsc();
}
