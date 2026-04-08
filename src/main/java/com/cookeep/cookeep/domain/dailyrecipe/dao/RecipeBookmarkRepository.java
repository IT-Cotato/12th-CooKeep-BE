package com.cookeep.cookeep.domain.dailyrecipe.dao;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeBookmark;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecipeBookmarkRepository extends JpaRepository<RecipeBookmark, Long> {
    Optional<RecipeBookmark> findByDailyRecipeAndUser(DailyRecipe dailyRecipe, User user);
    boolean existsByDailyRecipeAndUser(DailyRecipe dailyRecipe, User user);

    @Query("SELECT rb.dailyRecipe FROM RecipeBookmark rb " +
            "WHERE rb.user = :user " +
            "ORDER BY rb.createdAt DESC")
    Slice<DailyRecipe> findMyBookmarkedRecipes(@Param("user") User user, Pageable pageable);
}
