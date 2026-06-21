package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(name = "RecipeLikeResponse", description = "레시피 좋아요 토글 응답 DTO")
@Getter
@Builder
public class RecipeLikeResponseDto {
	@Schema(description = "데일리 레시피 ID")
	private Long dailyRecipeId;
	@Schema(description = "좋아요 여부")
	private boolean isLiked;
	@Schema(description = "현재 좋아요 수")
	private Integer likeCount;
	@Schema(description = "쿠키 리워드 정보 (좋아요 취소 시 null)")
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
