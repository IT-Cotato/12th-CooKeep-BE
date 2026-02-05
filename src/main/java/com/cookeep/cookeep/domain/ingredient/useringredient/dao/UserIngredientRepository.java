package com.cookeep.cookeep.domain.ingredient.useringredient.dao;

import com.cookeep.cookeep.domain.ingredient.common.Storage;
import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // --- 냉장고탭 ---
    // 홈화면 카테고리별 조회
    @Query("SELECT ui FROM UserIngredient ui WHERE ui.user.userId = :userId AND ui.storage = :storage ORDER BY ui.leftDays ASC")
    List<UserIngredient> findByUserIdAndStorage(
            @Param("userId") Long userId,
            @Param("storage") Storage storage
    );

    // 홈화면 좌우스크롤용 페이지
    Page<UserIngredient> findByUser_UserIdAndStorage(
            Long userId,
            Storage storage,
            Pageable pageable
    );

    // 식재료 상세조회
    @Query("SELECT ui FROM UserIngredient ui WHERE ui.ingredientId = :ingredientId AND ui.user.userId = :userId")
    Optional<UserIngredient> findByIngredientIdAndUserId(
            @Param("ingredientId") Long ingredientId,
            @Param("userId") Long userId
    );
}
