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

    // 이번 주 물주기 횟수 Top N 유저 조회
    @Query("SELECT w.user, COUNT(w) FROM WateringLog w " +
           "WHERE w.createdAt >= :weekStart AND w.createdAt < :weekEnd " +
           "GROUP BY w.user ORDER BY COUNT(w) DESC")
    List<Object[]> findTopWateringUsers(
            @Param("weekStart") LocalDateTime weekStart,
            @Param("weekEnd") LocalDateTime weekEnd,
            Pageable pageable);
}
