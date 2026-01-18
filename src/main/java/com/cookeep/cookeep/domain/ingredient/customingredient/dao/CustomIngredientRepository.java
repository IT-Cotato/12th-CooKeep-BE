package com.cookeep.cookeep.domain.ingredient.customingredient.dao;

import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomIngredientRepository extends JpaRepository<CustomIngredient, Long> {
    boolean existsByUserIdAndName(Long userId, String name);
    Optional<CustomIngredient> findByIdAndUserId(Long id, Long userId);
}
