package com.cookeep.cookeep.domain.cookeeps.application;

import com.cookeep.cookeep.api.dto.response.RankingResponseDto.RecipeRankDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.WateringRankDto;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.plant.dao.WateringLogRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RankingCacheService {

    private final WateringLogRepository wateringLogRepository;
    private final DailyRecipeRepository dailyRecipeRepository;

    @Cacheable(cacheNames = "wateringRanking", key = "#monthStart.toLocalDate().toString()")
    @Transactional(readOnly = true)
    public List<WateringRankDto> getWateringRanking(LocalDateTime monthStart, LocalDateTime monthEnd) {
        List<Object[]> results = wateringLogRepository.findTopWateringUsers(
            monthStart, monthEnd, PageRequest.of(0, 3));

        return IntStream.range(0, results.size())
            .mapToObj(index -> {
                Object[] row = results.get(index);
                User user = (User) row[0];
                Long wateringCount = (Long) row[1];
                String profileImageUrl = user.getProfilePlant() != null
                    ? user.getProfilePlant().getCurrentImageUrl()
                    : null;
                return WateringRankDto.builder()
                    .rank(index + 1)
                    .nickname(user.getNickname())
                    .profileImageUrl(profileImageUrl)
                    .wateringCount(wateringCount)
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "recipeRanking", key = "#weekStart.toLocalDate().toString()")
    @Transactional(readOnly = true)
    public List<RecipeRankDto> getRecipeRanking(LocalDateTime weekStart, LocalDateTime weekEnd) {
        List<DailyRecipe> results = dailyRecipeRepository.findTopRankedRecipes(
            weekStart, weekEnd, PageRequest.of(0, 3));

        return IntStream.range(0, results.size())
            .mapToObj(index -> {
                DailyRecipe recipe = results.get(index);
                return RecipeRankDto.builder()
                    .dailyRecipeId(recipe.getId())
                    .rank(index + 1)
                    .nickname(recipe.getUser().getNickname())
                    .title(recipe.getTitle())
                    .likeCount(recipe.getLikeCount().longValue())
                    .recipeImageUrl(recipe.getRecipeImageUrl())
                    .build();
            })
            .collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = "wateringRanking", allEntries = true)
    public void evictWateringRankingCache() {
    }

    @CacheEvict(cacheNames = "recipeRanking", allEntries = true)
    public void evictRecipeRankingCache() {
    }
}
