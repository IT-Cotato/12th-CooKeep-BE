package com.cookeep.cookeep.domain.dailyrecipe.dao;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DailyRecipeRepository extends JpaRepository<DailyRecipe, Long> {

    Optional<DailyRecipe> findByIdAndUser(Long id, User user); // 특정 레시피 조회 + 본인 소유 확인

    boolean existsByAiRecipe(AiRecipe aiRecipe); // AI 레시피 중복 등록 방지

    // 날짜 기반 데일리 레시피 조회 (최신순)
    @Query("SELECT dr FROM DailyRecipe dr WHERE dr.user = :user " +
           "AND dr.createdAt >= :start AND dr.createdAt < :end " +
           "ORDER BY dr.createdAt DESC")
    List<DailyRecipe> findByUserAndDateRange(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 캘린더 마킹용 월별 조회 (등록순 - 날짜별 첫 번째 레시피 추출용)
    @Query("SELECT dr FROM DailyRecipe dr WHERE dr.user = :user " +
           "AND dr.createdAt >= :start AND dr.createdAt < :end " +
           "ORDER BY dr.createdAt ASC")
    List<DailyRecipe> findByUserAndDateRangeAsc(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 이번 주차(월~일) 중 공개된 레시피만 조회
    @Query("SELECT dr FROM DailyRecipe dr WHERE dr.isPublic = true " +
            "AND dr.createdAt >= :start AND dr.createdAt < :end")
    Page<DailyRecipe> findWeeklyPublicRecipes(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    boolean existsByAiRecipe_Session_Id(Long sessionId);

    // 이번 주 공개 레시피 중 좋아요 랭킹 Top N 조회 (좋아요 내림차순, 동점 시 등록 오래된 순)
    @Query("SELECT dr FROM DailyRecipe dr WHERE dr.isPublic = true " +
            "AND dr.createdAt >= :start AND dr.createdAt < :end " +
            "ORDER BY dr.likeCount DESC, dr.createdAt ASC")
    List<DailyRecipe> findTopRankedRecipes(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

}
