package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeLikeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeLike;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeLikeService {

	private final RecipeLikeRepository recipeLikeRepository;
	private final DailyRecipeRepository dailyRecipeRepository;
	private final UserReader userReader;

	// 좋아요 추가/삭제 토글
	public boolean toggleLike(Long userId, Long dailyRecipeId) {
		User user = userReader.readById(userId);
		DailyRecipe dailyRecipe = dailyRecipeRepository.findById(dailyRecipeId)
			.orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));

		// 자신의 글에 좋아요 불가
		if (dailyRecipe.getUser().getUserId().equals(userId)) {
			throw new AppException(ErrorCode.CANNOT_LIKE_OWN_RECIPE);
		}

		// 이미 좋아요를 눌렀으면 삭제
		var existingLike = recipeLikeRepository.findByDailyRecipeAndUser(dailyRecipe, user);
		if (existingLike.isPresent()) {
			recipeLikeRepository.delete(existingLike.get());
			dailyRecipe.decrementLikeCount();
			return false; // 좋아요 취소
		}

		// 좋아요 추가
		RecipeLike recipeLike = RecipeLike.builder()
			.dailyRecipe(dailyRecipe)
			.user(user)
			.build();
		recipeLikeRepository.save(recipeLike);
		dailyRecipe.incrementLikeCount();
		return true; // 좋아요 추가
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
}
