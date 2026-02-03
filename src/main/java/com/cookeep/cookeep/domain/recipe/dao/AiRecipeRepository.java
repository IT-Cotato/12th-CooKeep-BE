package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecipeRepository extends JpaRepository<AiRecipe, Long> {

    // 세션 삭제 시 레시피 삭제
    void deleteBySessionId(Long sessionId);
}
