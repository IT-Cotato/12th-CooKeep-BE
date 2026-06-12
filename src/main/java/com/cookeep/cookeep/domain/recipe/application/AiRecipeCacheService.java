package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.entity.Difficulty;
import com.cookeep.cookeep.domain.recipe.entity.Feature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecipeCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "recipe:cache:";

    public String buildCacheKey(List<Long> ingredientIds, Feature feature, List<String> dislikedIngredients) {
        String sortedIds = ingredientIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining("_"));

        String dislikedPart = (dislikedIngredients == null || dislikedIngredients.isEmpty())
                ? "none"
                : dislikedIngredients.stream()
                .map(String::toLowerCase)
                .sorted()
                .collect(Collectors.joining("_"));

        return KEY_PREFIX + sortedIds + ":" + feature.name() + ":" + dislikedPart;
    }

    public GeminiRecipeResponseDto get(String cacheKey) {
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, GeminiRecipeResponseDto.class);
        } catch (Exception e) {
            log.warn("AI 레시피 캐시 역직렬화 실패. key={}", cacheKey, e);
            return null;
        }
    }

    public void put(String cacheKey, GeminiRecipeResponseDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(cacheKey, json, TTL);
            log.info("AI 레시피 캐시 저장. key={}", cacheKey);
        } catch (Exception e) {
            log.warn("AI 레시피 캐시 저장 실패. key={}", cacheKey, e);
        }
    }
}
