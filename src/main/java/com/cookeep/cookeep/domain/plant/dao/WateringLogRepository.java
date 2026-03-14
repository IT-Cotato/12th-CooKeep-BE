package com.cookeep.cookeep.domain.plant.dao;

import com.cookeep.cookeep.domain.plant.entity.WateringLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WateringLogRepository extends JpaRepository<WateringLog, Long> {
    boolean existsByUserUserId(Long userId);

    // 특정 유저의 이번 달 물주기 횟수 조회
    @Query("SELECT COUNT(w) FROM WateringLog w " +
           "WHERE w.user.userId = :userId " +
           "AND w.createdAt >= :monthStart AND w.createdAt < :monthEnd")
    Long countByUserAndMonth(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    // 이번 달 물주기 횟수 Top N 유저 조회
    @Query("SELECT w.user, COUNT(w) FROM WateringLog w " +
           "WHERE w.createdAt >= :monthStart AND w.createdAt < :monthEnd " +
           "GROUP BY w.user ORDER BY COUNT(w) DESC")
    List<Object[]> findTopWateringUsers(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd,
            Pageable pageable);
}
