package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRecipeRepository extends JpaRepository<AiRecipe, Long> {

    // 세션 삭제 시 레시피 삭제
    void deleteBySessionId(Long sessionId);

    // 유저의 채택된 AI 레시피 목록 조회 (최신순)
    List<AiRecipe> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
