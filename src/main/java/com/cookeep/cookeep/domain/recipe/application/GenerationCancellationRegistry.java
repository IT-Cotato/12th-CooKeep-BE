package com.cookeep.cookeep.domain.recipe.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 진행 중인 레시피 생성 작업(Gemini 호출 등)을 requestId 기준으로 등록/취소
 * 단일 인스턴스 전제
 */
@Slf4j
@Component
public class GenerationCancellationRegistry {

    private final Map<String, CompletableFuture<?>> futures = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> cancelledFlags = new ConcurrentHashMap<>();
    private final Map<String, Long> owners = new ConcurrentHashMap<>();

    // 레시피 생성 진입점에 호출해서 소유자를 기록
    public void registerOwner(String requestId, Long userId) {
        owners.put(requestId, userId);
    }

    // 작업 시작 시 호출. 아직 취소 플래그가 없으면 false로 초기화.
    public void register(String requestId, CompletableFuture<?> future) {
        cancelledFlags.putIfAbsent(requestId, new AtomicBoolean(false));
        futures.put(requestId, future);
    }

    // 앱의 취소 요청이 도착했을 때 호출.
    public boolean cancel(String requestId, Long userId) {
        Long ownerId = owners.get(requestId);

        // 소유자가 이미 기록돼 있는데 호출자와 다르면 거부
        if (ownerId != null && !ownerId.equals(userId)) {
            log.warn("취소 권한 없음. requestId={}, ownerId={}, userId={}", requestId, ownerId, userId);
            return false;
        }

        cancelledFlags.computeIfAbsent(requestId, k -> new AtomicBoolean()).set(true);
        CompletableFuture<?> future = futures.get(requestId);
        if (future != null) {
            log.info("레시피 생성 취소 요청 처리. requestId={}", requestId);
            return future.cancel(true);
        }
        log.info("레시피 생성 취소 요청 - 아직 등록된 작업 없음(선취소). requestId={}", requestId);
        return false; // future 등록 전에 취소하는 상황 방지 -> 플래그 이용 register/isCancelled에서 처리
    }

    // 각 단계 진입 전 체크포인트에서 호출
    public boolean isCancelled(String requestId) {
        AtomicBoolean flag = cancelledFlags.get(requestId);
        return flag != null && flag.get();
    }

    // 작업 종료 시 반드시 호출(메모리 누수 방지dyd)
    public void clear(String requestId) {
        futures.remove(requestId);
        cancelledFlags.remove(requestId);
        owners.remove(requestId);
    }
}
