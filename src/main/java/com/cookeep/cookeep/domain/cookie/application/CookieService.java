package com.cookeep.cookeep.domain.cookie.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.dao.CookieLogRepository;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CookieService {
    private final UserRepository userRepository;
    private final CookieLogRepository cookieLogRepository;

    // 현재 보유 쿠키 조회
    @Transactional(readOnly = true)
    public int getMyCookies(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getCookieCnt(); // User 엔티티에서 관리 중인 필드 사용
    }

    // 쿠키 사용/지급 공통 로직
    @Transactional
    public void updateCookie(Long userId, int amount, CookieLog.CookieLogType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 쿠키 차감 시 잔액 검증
        if (amount < 0 && user.getCookieCnt() < Math.abs(amount)) {
            throw new AppException(ErrorCode.NOT_ENOUGH_COOKIES);
        }

        // 1. 유저 쿠키 개수 업데이트
        user.updateCookieCnt(amount);

        // 2. 트랜잭션 로그 저장
        CookieLog log = CookieLog.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .build();
        cookieLogRepository.save(log);
    }
}