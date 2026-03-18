package com.cookeep.cookeep.domain.ingredient.useringredient.dao;

import com.cookeep.cookeep.domain.ingredient.useringredient.entity.RecentIngredientBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentIngredientBatchRepository extends JpaRepository<RecentIngredientBatch, Long> {

    Optional<RecentIngredientBatch> findByUser_UserId(Long userId);

    // 유저의 배치 목록 최신순 조회
    List<RecentIngredientBatch> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
