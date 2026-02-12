package com.cookeep.cookeep.domain.mycookeep.dao;

import com.cookeep.cookeep.domain.mycookeep.entity.WeeklyIngredientLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyIngredientLogRepository extends JpaRepository<WeeklyIngredientLog, Long> {

    boolean existsByUser_UserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    Optional<WeeklyIngredientLog> findByUser_UserIdAndWeekStartDateAndUserIngredientId(
            Long userId, LocalDate weekStartDate, Long userIngredientId);

    List<WeeklyIngredientLog> findAllByUser_UserIdAndWeekStartDateAndUserIngredientIdIn(
            Long userId, LocalDate weekStartDate, List<Long> userIngredientIds);

    @Query("""
        SELECT wil FROM WeeklyIngredientLog wil
        WHERE wil.weekStartDate = :weekStart
          AND wil.everNearExpiry = false
          AND wil.userIngredientId IN :ingredientIds
    """)
    List<WeeklyIngredientLog> findNotYetNearExpiryByWeekAndIngredientIds(
            @Param("weekStart") LocalDate weekStart,
            @Param("ingredientIds") List<Long> ingredientIds);

    @Query("""
        SELECT COUNT(wil) FROM WeeklyIngredientLog wil
        WHERE wil.user.userId = :userId AND wil.weekStartDate = :weekStart
    """)
    int countTotalByUserAndWeek(@Param("userId") Long userId,
                                @Param("weekStart") LocalDate weekStart);

    @Query("""
        SELECT COUNT(wil) FROM WeeklyIngredientLog wil
        WHERE wil.user.userId = :userId AND wil.weekStartDate = :weekStart
          AND wil.consumed = true
    """)
    int countConsumedByUserAndWeek(@Param("userId") Long userId,
                                   @Param("weekStart") LocalDate weekStart);

    @Query("""
        SELECT COUNT(wil) FROM WeeklyIngredientLog wil
        WHERE wil.user.userId = :userId AND wil.weekStartDate = :weekStart
          AND wil.everNearExpiry = true
    """)
    int countNearExpiryByUserAndWeek(@Param("userId") Long userId,
                                     @Param("weekStart") LocalDate weekStart);

    @Query("""
        SELECT COUNT(wil) FROM WeeklyIngredientLog wil
        WHERE wil.user.userId = :userId AND wil.weekStartDate = :weekStart
          AND wil.nearExpiryWhenConsumed = true
    """)
    int countConsumedNearExpiryByUserAndWeek(@Param("userId") Long userId,
                                             @Param("weekStart") LocalDate weekStart);
}
