package com.cookeep.cookeep.domain.ingredient.useringredient.dao;

import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserIngredientRepository extends JpaRepository<UserIngredient, Long> {

    @Query("""
        SELECT ui
        FROM UserIngredient ui
        WHERE ui.user.userId = :userId
          AND ui.type = :type
          AND ui.referenceId = :referenceId
        ORDER BY ui.ingredientId ASC
        LIMIT 1
    """)
    Optional<UserIngredient> findByUserIdAndTypeAndReferenceId(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("referenceId")Long referenceId
    );

    @Query("SELECT ui FROM UserIngredient ui WHERE ui.user.userId = :userId " +
            "AND ui.type = :type AND ui.referenceId IN :referenceIds")
    List<UserIngredient> findUserIngredients(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("referenceIds") List<Long> referenceIds
    );

}
