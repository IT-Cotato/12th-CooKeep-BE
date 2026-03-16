package com.cookeep.cookeep.domain.ingredient.useringredient.dao;

import com.cookeep.cookeep.domain.ingredient.useringredient.entity.RecentIngredientBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecentIngredientBatchRepository extends JpaRepository<RecentIngredientBatch, Long> {

    Optional<RecentIngredientBatch> findByUser_UserId(Long userId);
}
