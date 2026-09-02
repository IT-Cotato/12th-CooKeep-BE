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
public class WateringRankingResponseDto {
	private List<WateringRankDto> wateringRanking;
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
}
