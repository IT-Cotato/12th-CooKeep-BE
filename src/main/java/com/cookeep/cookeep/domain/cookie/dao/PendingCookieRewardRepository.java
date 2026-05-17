package com.cookeep.cookeep.domain.cookie.dao;

import com.cookeep.cookeep.domain.cookie.entity.PendingCookieReward;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PendingCookieRewardRepository extends JpaRepository<PendingCookieReward, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PendingCookieReward p WHERE p.pendingRewardId = :id")
    Optional<PendingCookieReward> findByIdForUpdate(@Param("id") Long id);
}
