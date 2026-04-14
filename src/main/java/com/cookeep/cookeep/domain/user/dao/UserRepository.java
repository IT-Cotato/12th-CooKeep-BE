package com.cookeep.cookeep.domain.user.dao;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.plant.entity.PlantStatus;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumber(String phoneNumber);

    // 푸시 알림 동의한 사용자 조회
    @Query("SELECT u FROM User u WHERE u.marketingPush = true")
    List<User> findAllByMarketingPushTrue();

    // 특정 plantStatus이고 marketingConsent 동의한 + 키우는 식물 보유 유저 ID 조회 (식물 상태 푸시 알림용)
    @Query("""
        SELECT DISTINCT u.userId
        FROM User u
        JOIN UserPlant up ON up.user.userId = u.userId
        WHERE u.plantStatus = :plantStatus
          AND u.marketingConsent = true
          AND up.isHarvested = false
    """)
    List<Long> findUserIdsByPlantStatusAndMarketingConsent(
            @Param("plantStatus") PlantStatus plantStatus);

    boolean existsByEmail(String email);
}
