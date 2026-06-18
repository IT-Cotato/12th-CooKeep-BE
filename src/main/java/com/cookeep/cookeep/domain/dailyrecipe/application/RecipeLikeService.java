package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.api.dto.response.CookeepsFeedResponseDto;
import com.cookeep.cookeep.api.dto.response.CookieRewardDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeLikeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeLike;
import com.cookeep.cookeep.domain.onboarding.application.WeeklyGoalService;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeLikeService {

	private final RecipeLikeRepository recipeLikeRepository;
	private final DailyRecipeRepository dailyRecipeRepository;
	private final UserReader userReader;
	private final WeeklyGoalService weeklyGoalService;
	private final CookieService cookieService;

	public record ToggleLikeResult(boolean isLiked, CookieRewardDto reward) {}

	// 좋아요 추가/삭제 토글
	public ToggleLikeResult toggleLike(Long userId, Long dailyRecipeId) {
		User user = userReader.readById(userId);
		DailyRecipe dailyRecipe = dailyRecipeRepository.findById(dailyRecipeId)
			.orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));

		// 자신의 글에 좋아요 불가
		if (dailyRecipe.getUser().getUserId().equals(userId)) {
			throw new AppException(ErrorCode.CANNOT_LIKE_OWN_RECIPE);
		}

		var existingLike = recipeLikeRepository.findByDailyRecipeAndUser(dailyRecipe, user);

		// 이미 좋아요를 눌렀으면 삭제
		if (existingLike.isPresent()) {
			recipeLikeRepository.delete(existingLike.get());
			dailyRecipe.decrementLikeCount();
			weeklyGoalService.handleGoalUndo(userId, GoalActionType.RECIPE_LIKE);
			return new ToggleLikeResult(false, null);
		}

		// 좋아요 추가
		RecipeLike recipeLike = RecipeLike.builder()
			.dailyRecipe(dailyRecipe)
			.user(user)
			.build();
		recipeLikeRepository.save(recipeLike);
		dailyRecipe.incrementLikeCount();
		boolean goalAchieved = weeklyGoalService.handleGoalProgress(userId, GoalActionType.RECIPE_LIKE);
		List<CookieLog.CookieLogType> types = goalAchieved
				? List.of(CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE) : List.of();
		int points = goalAchieved ? CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE.getDefaultAmount() : 0;
		CookieRewardDto reward = CookieRewardDto.builder()
				.granted(goalAchieved).points(points).types(types)
				.currentCookieCount(cookieService.getMyCookies(userId))
				.build();
		return new ToggleLikeResult(true, reward);
	}

	// 특정 레시피의 좋아요 수 조회
	@Transactional(readOnly = true)
	public long getLikeCount(Long dailyRecipeId) {
		DailyRecipe dailyRecipe = dailyRecipeRepository.findById(dailyRecipeId)
			.orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));
		return recipeLikeRepository.countByDailyRecipe(dailyRecipe);
	}

	// 특정 사용자가 특정 레시피에 좋아요를 눌렀는지 확인
	@Transactional(readOnly = true)
	public boolean isLiked(Long userId, Long dailyRecipeId) {
		User user = userReader.readById(userId);
		DailyRecipe dailyRecipe = dailyRecipeRepository.findById(dailyRecipeId)
			.orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));
		return recipeLikeRepository.existsByDailyRecipeAndUser(dailyRecipe, user);
	}

	@Transactional(readOnly = true)
	public Slice<CookeepsFeedResponseDto> getMyLikedRecipes(Long userId, Pageable pageable) {
		User user = userReader.readById(userId);

		// 좋아요 많은 순으로 조회 (Repository의 쿼리 결과)
		Slice<DailyRecipe> recipes = recipeLikeRepository.findMyLikedRecipes(user, pageable);

		return recipes.map(recipe -> CookeepsFeedResponseDto.builder()
				.dailyRecipeId(recipe.getId())
				.title(recipe.getTitle())
				.likeCount(recipe.getLikeCount())
				.recipeImageUrl(recipe.getRecipeImageUrl())
				.createdAt(recipe.getCreatedAt())
				.build());
	}
}
