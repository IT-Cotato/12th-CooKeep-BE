package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeLikeResponseDto {
	private Long dailyRecipeId;
	private boolean isLiked; // true: 좋아요 추가, false: 좋아요 취소
	private Integer likeCount; // 현재 좋아요 수

	public static RecipeLikeResponseDto from(Long dailyRecipeId, boolean isLiked, Integer likeCount) {
		return RecipeLikeResponseDto.builder()
			.dailyRecipeId(dailyRecipeId)
			.isLiked(isLiked)
			.likeCount(likeCount)
			.build();
	}
}
