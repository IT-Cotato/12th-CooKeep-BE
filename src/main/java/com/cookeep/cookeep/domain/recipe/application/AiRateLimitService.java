package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiRateLimitService {

    // 유저 요청 기록 (아이디로 구분)
    private final Map<Long, List<Long>> requestLogs = new ConcurrentHashMap<>();

    // 제한 설정
    private static final int LIMIT = 3;         // 3회/m
    private static final long WINDOW = 60_000;  // 1분

    public void validate(Long userId) {
        long now = System.currentTimeMillis();

        requestLogs.putIfAbsent(userId, new ArrayList<>());
        List<Long> logs = requestLogs.get(userId);

        // 오래된 요청 제거 (1분 이전)
        logs.removeIf(time -> now - time > WINDOW);

        // 제한 초과
        if (logs.size() >= LIMIT) {
            throw new AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }

        // 현재 요청 기록
        logs.add(now);
    }
}
