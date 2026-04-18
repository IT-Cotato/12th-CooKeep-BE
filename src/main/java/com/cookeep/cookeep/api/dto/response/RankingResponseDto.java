package com.cookeep.cookeep.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponseDto {
	private List<WateringRankDto> wateringRanking;
	private List<RecipeRankDto> recipeRanking;
	private Long myWateringCount;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class WateringRankDto {
		private Integer rank;
		private String nickname;
		private String profileImageUrl;
		private Long wateringCount;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class RecipeRankDto {
		private Long dailyRecipeId;
		private Integer rank;
		private String nickname;
		private String title;
		private Long likeCount;
		private String recipeImageUrl;
	}
}
