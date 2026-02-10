package com.cookeep.cookeep.domain.dailyrecipe.dao;

import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}
