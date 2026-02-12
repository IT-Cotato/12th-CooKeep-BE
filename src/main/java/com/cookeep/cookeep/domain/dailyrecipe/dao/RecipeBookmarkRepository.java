package com.cookeep.cookeep.domain.dailyrecipe.dao;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeBookmark;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeBookmarkRepository extends JpaRepository<RecipeBookmark, Long> {
    Optional<RecipeBookmark> findByDailyRecipeAndUser(DailyRecipe dailyRecipe, User user);
    boolean existsByDailyRecipeAndUser(DailyRecipe dailyRecipe, User user);
}
