package com.cookeep.cookeep.domain.ingredient.useringredient.dao;

import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    List<UserIngredient> findAllByIngredientIdInAndUser_UserId(
            List<Long> ingredientIds,
            Long userId
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

    // 카테고리별 분류 + 이름 검색
    @Query("""
        SELECT ui FROM UserIngredient ui
        WHERE ui.user.userId = :userId
        AND (:storage IS NULL OR ui.storage = :storage)
        AND (:searchQuery IS NULL OR 
            EXISTS (
                SELECT 1 FROM DefaultIngredient di 
                WHERE di.id = ui.referenceId 
                AND ui.type = 'DEFAULT'
                AND LOWER(di.ingredient) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
            )
            OR EXISTS (
                SELECT 1 FROM CustomIngredient ci 
                WHERE ci.id = ui.referenceId 
                AND ui.type = 'CUSTOM'
                AND LOWER(ci.name) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
            )
        )
    """)
    Page<UserIngredient> searchIngredients(
            @Param("userId") Long userId,
            @Param("searchQuery") String searchQuery,
            @Param("storage") Storage storage,
            Pageable pageable
    );

    // --- 푸시 알림 전송 ---
    // 유통기한 임박 식재료 존재 여부 확인
    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1
            FROM UserIngredient ui
            WHERE ui.user.userId = :userId
              AND ui.expirationDate = :expirationDate
        ) THEN true ELSE false END
    """)
    boolean existsByUserIdAndExpirationDate(
            @Param("userId") Long userId,
            @Param("expirationDate") LocalDate expirationDate
    );

    // 유통기한 당일(D-0) 식재료 조회
    @Query("""
        SELECT ui
        FROM UserIngredient ui
        WHERE ui.user.userId = :userId
        AND ui.expirationDate = :expirationDate
        ORDER BY ui.ingredientId ASC
    """)
    List<UserIngredient> findByUserIdAndExpirationDateOrderByIngredientIdAsc(
            @Param("userId") Long userId,
            @Param("expirationDate") LocalDate expirationDate
    );

}
