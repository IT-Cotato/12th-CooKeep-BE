package com.cookeep.cookeep.domain.recipe.dao;

import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiRecipeRepository extends JpaRepository<AiRecipe, Long> {

    // 세션 삭제 시 레시피 삭제
    void deleteBySessionId(Long sessionId);

    // 채택된 AI 레시피 전체 조회 (최신순, session fetch join으로 isPinned 조회)
    @EntityGraph(attributePaths = {"session"})
    @Query("SELECT ar FROM AiRecipe ar WHERE ar.userId = :userId ORDER BY ar.createdAt DESC")
    List<AiRecipe> findAllByUserId(@Param("userId") Long userId);

    // 유저의 채택된 AI 레시피 상세 조회 (session fetch join)
    @EntityGraph(attributePaths = {"session"})
    Optional<AiRecipe> findByIdAndUserId(Long id, Long userId);
}
