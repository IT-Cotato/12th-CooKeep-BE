package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeLikeResponseDto {
	private Long dailyRecipeId;
	private boolean isLiked; // true: 좋아요 추가, false: 좋아요 취소
	private Integer likeCount; // 현재 좋아요 수
	private boolean weeklyGoalAchieved; // 이번 액션으로 주간 목표 달성 여부

	public static RecipeLikeResponseDto from(Long dailyRecipeId, boolean isLiked, Integer likeCount) {
		return from(dailyRecipeId, isLiked, likeCount, false);
	}

	public static RecipeLikeResponseDto from(Long dailyRecipeId, boolean isLiked, Integer likeCount, boolean weeklyGoalAchieved) {
		return RecipeLikeResponseDto.builder()
			.dailyRecipeId(dailyRecipeId)
			.isLiked(isLiked)
			.likeCount(likeCount)
			.weeklyGoalAchieved(weeklyGoalAchieved)
			.build();
	}
}
