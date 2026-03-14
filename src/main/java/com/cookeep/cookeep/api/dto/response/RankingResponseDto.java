package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RankingResponseDto {
	private List<WateringRankDto> wateringRanking;
	private List<RecipeRankDto> recipeRanking;
	private Long myWateringCount;

	@Getter
	@Builder
	public static class WateringRankDto {
		private Integer rank;
		private String nickname;
		private String profileImageUrl;
		private Long wateringCount;
	}

	@Getter
	@Builder
	public static class RecipeRankDto {
		private Long dailyRecipeId;
		private Integer rank;
		private String title;
		private Long likeCount;
		private String recipeImageUrl;
	}
}
