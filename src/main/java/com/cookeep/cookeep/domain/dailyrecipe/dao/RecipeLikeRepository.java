package com.cookeep.cookeep.domain.dailyrecipe.dao;

import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeLike;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecipeLikeRepository extends JpaRepository<RecipeLike, Long> {

	// 이번 주 등록된 레시피 중 이번 주에 받은 좋아요 Top N 조회
	@Query("SELECT rl.dailyRecipe, COUNT(rl) FROM RecipeLike rl " +
		   "WHERE rl.dailyRecipe.createdAt >= :weekStart AND rl.dailyRecipe.createdAt < :weekEnd " +
		   "AND rl.createdAt >= :weekStart AND rl.createdAt < :weekEnd " +
		   "GROUP BY rl.dailyRecipe ORDER BY COUNT(rl) DESC")
	List<Object[]> findTopLikedRecipes(
		@Param("weekStart") LocalDateTime weekStart,
		@Param("weekEnd") LocalDateTime weekEnd,
		Pageable pageable);

	// 특정 사용자가 특정 레시피에 좋아요를 눌렀는지 확인
	Optional<RecipeLike> findByDailyRecipeAndUser(DailyRecipe dailyRecipe, User user);

	// 특정 레시피의 좋아요 수
	long countByDailyRecipe(DailyRecipe dailyRecipe);

	// 특정 사용자가 특정 레시피에 좋아요를 눌렀는지 boolean으로 확인
	boolean existsByDailyRecipeAndUser(DailyRecipe dailyRecipe, User user);

	// RecipeLike 테이블을 거쳐서 내가 좋아요를 누른 레시피들 조회
	@Query("SELECT rl.dailyRecipe FROM RecipeLike rl " +
			"WHERE rl.user = :user " +
			"ORDER BY rl.dailyRecipe.likeCount DESC, rl.dailyRecipe.createdAt DESC")
	Slice<DailyRecipe> findMyLikedRecipes(@Param("user") User user, Pageable pageable);
}
