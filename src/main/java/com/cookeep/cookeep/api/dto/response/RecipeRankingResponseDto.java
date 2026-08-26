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
public class RecipeRankingResponseDto {
	private List<RecipeRankDto> recipeRanking;

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
		private String description;
	}
}
