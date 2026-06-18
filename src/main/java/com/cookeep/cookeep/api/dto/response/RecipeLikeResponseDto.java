package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeLikeResponseDto {
	private Long dailyRecipeId;
	private boolean isLiked;
	private Integer likeCount;
	private CookieRewardDto reward;

	public static RecipeLikeResponseDto from(Long dailyRecipeId, boolean isLiked, Integer likeCount) {
		return RecipeLikeResponseDto.builder()
			.dailyRecipeId(dailyRecipeId)
			.isLiked(isLiked)
			.likeCount(likeCount)
			.build();
	}

	public static RecipeLikeResponseDto from(Long dailyRecipeId, boolean isLiked, Integer likeCount, CookieRewardDto reward) {
		return RecipeLikeResponseDto.builder()
			.dailyRecipeId(dailyRecipeId)
			.isLiked(isLiked)
			.likeCount(likeCount)
			.reward(reward)
			.build();
	}
}
