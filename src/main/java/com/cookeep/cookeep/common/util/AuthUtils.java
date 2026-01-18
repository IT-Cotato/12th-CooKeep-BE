package com.cookeep.cookeep.common.util;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

public final class AuthUtils {

    private AuthUtils() { /* util */ }

    /**
     * Authorization 헤더에서 Bearer 토큰을 안전하게 추출합니다.
     *
     * - null 체크
     * - "Bearer " 접두사 확인 (대소문자 무시)
     * - 공백 제거
     * - 빈 토큰 검증
     *
     * @param authorizationHeader Authorization 헤더 값
     * @return 추출된 토큰
     * @throws AppException ErrorCode.UNAUTHORIZED
     */
    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String header = authorizationHeader.trim();
        if (header.length() < 7) { // "Bearer " 보다 짧으면 토큰 없음
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // case-insensitive로 "Bearer " 접두사 체크 (7글자)
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            if (token.isEmpty()) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            return token;
        } else {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
