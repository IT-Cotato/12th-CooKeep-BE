package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.IngredientDetailDto;
import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
import com.cookeep.cookeep.domain.recipe.entity.Feature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiQueueService {

    private final GeminiService geminiService;
    private final GenerationCancellationRegistry cancellationRegistry;

    // 동시 Gemini 호출 최대 3개로 제한
    private static final int MAX_CONCURRENT = 3;
    private static final int QUEUE_TIMEOUT_SECONDS = 90;

    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT, true);

    /**
     * Semaphore로 동시 호출 수를 제어합니다.
     * 슬롯이 없으면 최대 90초 대기 후 타임아웃 에러를 반환합니다.
     */
    public GeminiRecipeResponseDto generateRecipe(
            String requestId,
            List<IngredientDetailDto> ingredients,
            //Difficulty difficulty,
            Feature feature,
            List<String> dislikedIngredients) {

        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Gemini 큐 대기 타임아웃. 현재 대기 수={}", semaphore.getQueueLength());
                throw new AppException(ErrorCode.AI_SEARCH_FAILED);
            }
            log.info("Gemini 슬롯 획득. 남은 슬롯={}", semaphore.availablePermits());

            if (cancellationRegistry.isCancelled(requestId)) {
                log.info("Gemini 호출 전 취소 감지. requestId={}", requestId);
                throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
            }

            CompletableFuture<GeminiRecipeResponseDto> future =
                    geminiService.generateRecipe(ingredients, feature, dislikedIngredients);
            cancellationRegistry.register(requestId, future);

            return future.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        } catch (CancellationException e) {
            log.info("Gemini 호출 취소됨. requestId={}", requestId);
            throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
        } catch (ExecutionException e) {
            throw unwrap(e);
        } finally {
            if (acquired) {
                semaphore.release();
                log.info("Gemini 슬롯 반환. 남은 슬롯={}", semaphore.availablePermits());
            }
        }
    }

    public GeminiRecipeResponseDto generateRecipeWithExclusion(
            String requestId,
            List<IngredientDetailDto> ingredients,
            //Difficulty difficulty,
            Feature feature,
            List<String> excludedTitles,
            List<String> dislikedIngredients) {

        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Gemini 큐 대기 타임아웃. 현재 대기 수={}", semaphore.getQueueLength());
                throw new AppException(ErrorCode.AI_SEARCH_FAILED);
            }
            if (cancellationRegistry.isCancelled(requestId)) {
                throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
            }
            CompletableFuture<GeminiRecipeResponseDto> future =
                    geminiService.generateRecipeWithExclusion(
                            ingredients, feature, excludedTitles, dislikedIngredients);
            cancellationRegistry.register(requestId, future);

            return future.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        } catch (CancellationException e) {
            throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
        } catch (ExecutionException e) {
            throw unwrap(e);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    public GeminiRecipeResponseDto generateRandomRecipe(
            String requestId,
            List<IngredientDetailDto> allIngredients,
            List<String> dislikedIngredients) {

        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Gemini 큐 대기 타임아웃(랜덤). 현재 대기 수={}", semaphore.getQueueLength());
                throw new AppException(ErrorCode.AI_SEARCH_FAILED);
            }
            log.info("Gemini 슬롯 획득(랜덤). 남은 슬롯={}", semaphore.availablePermits());

            if (cancellationRegistry.isCancelled(requestId)) {
                throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
            }

            CompletableFuture<GeminiRecipeResponseDto> future =
                    geminiService.generateRandomRecipe(allIngredients, dislikedIngredients);
            cancellationRegistry.register(requestId, future);

            return future.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        } catch (CancellationException e) {
            throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
        } catch (ExecutionException e) {
            throw unwrap(e);
        } finally {
            if (acquired) {
                semaphore.release();
                log.info("Gemini 슬롯 반환(랜덤). 남은 슬롯={}", semaphore.availablePermits());
            }
        }
    }

    public GeminiRecipeResponseDto generateRandomRecipeWithExclusion(
            String requestId,
            List<IngredientDetailDto> allIngredients,
            List<String> dislikedIngredients,
            List<String> excludedTitles) {

        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Gemini 큐 대기 타임아웃(랜덤 재요청). 현재 대기 수={}", semaphore.getQueueLength());
                throw new AppException(ErrorCode.AI_SEARCH_FAILED);
            }

            if (cancellationRegistry.isCancelled(requestId)) {
                throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
            }

            CompletableFuture<GeminiRecipeResponseDto> future =
                    geminiService.generateRandomRecipeWithExclusion(
                            allIngredients, dislikedIngredients, excludedTitles);
            cancellationRegistry.register(requestId, future);

            return future.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        } catch (CancellationException e) {
            throw new AppException(ErrorCode.AI_GENERATION_CANCELLED);
        } catch (ExecutionException e) {
            throw unwrap(e);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    // 500에러 방지. AppException으로 명시적 에러 처리
    private AppException unwrap(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof AppException appException) {
            return appException;
        }
        log.error("Gemini 비동기 호출 중 예상치 못한 예외", e);
        return new AppException(ErrorCode.AI_SEARCH_FAILED);
    }
}
