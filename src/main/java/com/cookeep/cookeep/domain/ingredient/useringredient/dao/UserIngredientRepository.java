package com.cookeep.cookeep.domain.ingredient.useringredient.dao;

import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {
}
