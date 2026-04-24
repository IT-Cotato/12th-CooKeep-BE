package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.IngredientDetailDto;
import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiQueueService {

    private final GeminiService geminiService;

    // 동시 Gemini 호출 최대 3개로 제한
    private static final int MAX_CONCURRENT = 3;
    private static final int QUEUE_TIMEOUT_SECONDS = 90;

    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT, true);

    /**
     * Semaphore로 동시 호출 수를 제어합니다.
     * 슬롯이 없으면 최대 90초 대기 후 타임아웃 에러를 반환합니다.
     */
    public GeminiRecipeResponseDto generateRecipe(
            List<IngredientDetailDto> ingredients,
            Difficulty difficulty,
            List<String> dislikedIngredients) {

        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Gemini 큐 대기 타임아웃. 현재 대기 수={}", semaphore.getQueueLength());
                throw new AppException(ErrorCode.AI_SEARCH_FAILED);
            }
            log.info("Gemini 슬롯 획득. 남은 슬롯={}", semaphore.availablePermits());
            return geminiService.generateRecipe(ingredients, difficulty, dislikedIngredients);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        } finally {
            if (acquired) {
                semaphore.release();
                log.info("Gemini 슬롯 반환. 남은 슬롯={}", semaphore.availablePermits());
            }
        }
    }

    public GeminiRecipeResponseDto generateRecipeWithExclusion(
            List<IngredientDetailDto> ingredients,
            Difficulty difficulty,
            List<String> excludedTitles,
            List<String> dislikedIngredients) {

        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(QUEUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Gemini 큐 대기 타임아웃. 현재 대기 수={}", semaphore.getQueueLength());
                throw new AppException(ErrorCode.AI_SEARCH_FAILED);
            }
            return geminiService.generateRecipeWithExclusion(
                    ingredients, difficulty, excludedTitles, dislikedIngredients);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
}
