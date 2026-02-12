package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.api.dto.response.WeeklyRecipeResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeBookmarkRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeBookmark;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeBookmarkService {

    private final RecipeBookmarkRepository recipeBookmarkRepository;
    private final DailyRecipeRepository dailyRecipeRepository;
    private final UserReader userReader;

    public boolean toggleBookmark(Long userId, Long dailyRecipeId) {
        User user = userReader.readById(userId);
        DailyRecipe dailyRecipe = dailyRecipeRepository.findById(dailyRecipeId)
                .orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));

        // 자신의 글에는 북마크 불가
        if (dailyRecipe.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_BOOKMARK_OWN_RECIPE); // 적절한 에러코드 사용
        }

        var existingBookmark = recipeBookmarkRepository.findByDailyRecipeAndUser(dailyRecipe, user);

        if (existingBookmark.isPresent()) {
            recipeBookmarkRepository.delete(existingBookmark.get());
            return false; // 북마크 취소
        }

        RecipeBookmark bookmark = RecipeBookmark.builder()
                .dailyRecipe(dailyRecipe)
                .user(user)
                .build();
        recipeBookmarkRepository.save(bookmark);
        return true; // 북마크 추가
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long dailyRecipeId) {
        User user = userReader.readById(userId);
        return recipeBookmarkRepository.existsByDailyRecipeAndUser(
                dailyRecipeRepository.getReferenceById(dailyRecipeId), user);
    }

    @Transactional(readOnly = true)
    public Page<WeeklyRecipeResponseDto> getMyBookmarkedRecipes(Long userId, Pageable pageable) {
        User user = userReader.readById(userId);

        Page<DailyRecipe> recipes = recipeBookmarkRepository.findMyBookmarkedRecipes(user, pageable);

        return recipes.map(recipe -> WeeklyRecipeResponseDto.builder()
                .dailyRecipeId(recipe.getId())
                .title(recipe.getTitle())
                .likeCount(recipe.getLikeCount())
                .recipeImageUrl(recipe.getRecipeImageUrl())
                .build());
    }
}
