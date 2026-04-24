package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRateLimitService {

    private final StringRedisTemplate redisTemplate;

    // 유저 요청 기록 (아이디로 구분)
    private final Map<Long, List<Long>> requestLogs = new ConcurrentHashMap<>();

    // 제한 설정
    private static final int LIMIT = 3;         // 3회/m
    private static final Duration WINDOW = Duration.ofMinutes(1); // 1분
    private static final String KEY_PREFIX = "rate:limit:ai:";


    public void validate(Long userId) {
        String key = KEY_PREFIX + userId;

        // INCR는 원자적으로 값을 증가시키고, 키가 없으면 1로 초기화
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 최초 요청(count == 1)일 때만 TTL 설정
        if (count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count > LIMIT) {
            log.warn("AI Rate Limit 초과. userId={}, count={}", userId, count);
            throw new AppException(ErrorCode.USER_RATE_LIMIT_EXCEEDED);
        }
    }
}
