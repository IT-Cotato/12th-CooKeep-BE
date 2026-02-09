package com.cookeep.cookeep.domain.cookie.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.dao.CookieLogRepository;
import com.cookeep.cookeep.domain.cookie.dao.DailyCookieGrantRepository;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.cookie.entity.DailyCookieGrant;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieService {
    private final UserRepository userRepository;
    private final CookieLogRepository cookieLogRepository;
    private final DailyCookieGrantRepository dailyCookieGrantRepository;

    // 현재 보유 쿠키 조회
    @Transactional(readOnly = true)
    public int getMyCookies(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getCookieCnt(); // User 엔티티에서 관리 중인 필드 사용
    }

    // 쿠키 사용/지급 공통 로직
    @Transactional
    public void updateCookie(Long userId, CookieLog.CookieLogType type) {
        int amount = type.getDefaultAmount();

        // 1. 비관적 락을 사용하여 유저 정보를 가져옴 (다른 트랜잭션 대기)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. 검증
        if (amount == 0 ||
                amount < 0 && (user.getCookieCnt() < Math.abs(amount))) {
            throw new AppException(ErrorCode.NOT_ENOUGH_COOKIES);
        }

        // 3. 업데이트 및 로그 저장
        user.updateCookieCnt(amount);

        CookieLog log = CookieLog.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .build();
        cookieLogRepository.save(log);
    }

    // 일일 제한있는 쿠키 지급
    @Transactional
    public boolean grantDailyCookie(Long userId, CookieLog.CookieLogType type) {
        LocalDate today = LocalDate.now();

        // 1. 오늘 이미 지급되었는지 확인
        boolean alreadyGranted = dailyCookieGrantRepository.existsByUser_UserIdAndGrantTypeAndGrantDate(
                userId, type, today
        );

        if (alreadyGranted) {
            log.info("Daily cookie already granted today for user {}: type={}", userId, type);
            return false;
        }

        // 2. 유저 조회 (비관적 락)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        int amount = type.getDefaultAmount();

        // 3. 쿠키 업데이트
        user.updateCookieCnt(amount);

        // 4. 쿠키 로그 저장
        CookieLog log = CookieLog.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .build();
        cookieLogRepository.save(log);

        // 5. 일일 지급 기록 저장
        DailyCookieGrant grant = DailyCookieGrant.builder()
                .user(user)
                .grantType(type)
                .grantDate(today)
                .build();
        dailyCookieGrantRepository.save(grant);

        return true;
    }

    // 오늘 특정 타입의 쿠키를 이미 받았는지 확인
    @Transactional(readOnly = true)
    public boolean isGrantedToday(Long userId, CookieLog.CookieLogType type) {
        return dailyCookieGrantRepository.existsByUser_UserIdAndGrantTypeAndGrantDate(
                userId, type, LocalDate.now()
        );
    }
}