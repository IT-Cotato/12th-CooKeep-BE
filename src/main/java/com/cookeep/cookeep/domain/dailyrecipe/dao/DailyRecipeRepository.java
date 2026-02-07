package com.cookeep.cookeep.domain.dailyrecipe.dao;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyRecipeRepository extends JpaRepository<DailyRecipe, Long> {

    Optional<DailyRecipe> findByIdAndUser(Long id, User user); // 특정 레시피 조회 + 본인 소유 확인
}
