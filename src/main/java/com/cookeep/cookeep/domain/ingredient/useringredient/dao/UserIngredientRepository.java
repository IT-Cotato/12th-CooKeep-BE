package com.cookeep.cookeep.domain.ingredient.useringredient.dao;

import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {

    Optional<UserIngredient> findByTypeAndReferenceId(
            Type type,
            Long referenceId
    );

    List<UserIngredient> findByUserIdAndTypeAndReferenceIdIn(
            Long userId,
            Type type,
            List<Long> referenceIds
    );
}
