package com.cookeep.cookeep.domain.user.dao;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
