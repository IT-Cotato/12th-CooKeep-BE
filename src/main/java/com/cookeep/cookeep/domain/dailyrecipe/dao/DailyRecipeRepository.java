package com.cookeep.cookeep.domain.dailyrecipe.dao;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyRecipeRepository extends JpaRepository<DailyRecipe, Long> {

    List<DailyRecipe> findAllByUserOrderByCreatedAtDesc(User user); // 사용자의 데일리 레시피 목록 (최신순)

    Optional<DailyRecipe> findByIdAndUser(Long id, User user); // 특정 레시피 조회 + 본인 소유 확인
}
