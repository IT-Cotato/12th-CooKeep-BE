package com.cookeep.cookeep.domain.cookie.dao;

import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.cookie.entity.DailyCookieGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyCookieGrantRepository extends JpaRepository<DailyCookieGrant, Long> {

    // 특정 타입의 쿠키 지급 여부 확인
    boolean existsByUser_UserIdAndGrantTypeAndGrantDate(
            Long userId,
            CookieLog.CookieLogType grantType,
            LocalDate grantDate
    );

    // 특정 유저의 특정 타입/날짜 지급 기록 조회
    Optional<DailyCookieGrant> findByUser_UserIdAndGrantTypeAndGrantDate(
            Long userId,
            CookieLog.CookieLogType grantType,
            LocalDate grantDate
    );

}
