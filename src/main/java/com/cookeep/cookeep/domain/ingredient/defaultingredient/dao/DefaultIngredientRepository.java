package com.cookeep.cookeep.domain.ingredient.defaultingredient.dao;

import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultIngredientRepository extends JpaRepository<DefaultIngredient, Long> {
}
