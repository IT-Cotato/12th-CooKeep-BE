package com.cookeep.cookeep.domain.cookeeps.application;

import com.cookeep.cookeep.api.dto.response.RankingResponseDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.RecipeRankDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.WateringRankDto;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeLikeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.plant.dao.WateringLogRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CookeepsService {

	private final WateringLogRepository wateringLogRepository;
	private final RecipeLikeRepository recipeLikeRepository;

	@Transactional(readOnly = true)
	public RankingResponseDto getRanking() {
		LocalDateTime weekStart = LocalDate.now()
			.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			.atStartOfDay();
		LocalDateTime weekEnd = weekStart.plusDays(7);

		List<WateringRankDto> wateringRanking = getWateringRanking(weekStart, weekEnd);
		List<RecipeRankDto> recipeRanking = getRecipeRanking(weekStart, weekEnd);

		return RankingResponseDto.builder()
			.wateringRanking(wateringRanking)
			.recipeRanking(recipeRanking)
			.build();
	}

	private List<WateringRankDto> getWateringRanking(LocalDateTime weekStart, LocalDateTime weekEnd) {
		// 1단계: 이번 주 물주기 상위 3명 조회
		List<Object[]> results = wateringLogRepository.findTopWateringUsers(
			weekStart, weekEnd, PageRequest.of(0, 3));
		
		// 2단계: 인덱스를 포함한 스트림 처리
		return IntStream.range(0, results.size()) // 0부터 results 크기-1까지
			.mapToObj(index -> {
				Object[] row = results.get(index);
				User user = (User) row[0]; // 유저 객체 추출

				// 프로필 이미지 URL 가져오기
				String profileImageUrl = user.getProfilePlant() != null
					? user.getProfilePlant().getCurrentImageUrl()
					: null;
				
				// DTO로 변환 (rank = 인덱스 + 1 → 1, 2, 3)
				return WateringRankDto.builder()
					.rank(index + 1)
					.nickname(user.getNickname())
					.profileImageUrl(profileImageUrl)
					.build();
			})
			.toList();
	}

	private List<RecipeRankDto> getRecipeRanking(LocalDateTime weekStart, LocalDateTime weekEnd) {
		// 1단계: 이번 주 좋아요 상위 3개 레시피 조회
		List<Object[]> results = recipeLikeRepository.findTopLikedRecipes(
			weekStart, weekEnd, PageRequest.of(0, 3));
		
		// 2단계: 인덱스를 포함한 스트림 처리
		return IntStream.range(0, results.size())
			.mapToObj(index -> {
				Object[] row = results.get(index);
				DailyRecipe recipe = (DailyRecipe) row[0]; // 레시피 객체 추출
				Long likeCount = (Long) row[1]; // 좋아요 수 추출
				
				// DTO로 변환 (rank = 인덱스 + 1 → 1, 2, 3)
				return RecipeRankDto.builder()
					.rank(index + 1)
					.title(recipe.getTitle())
					.likeCount(likeCount)
					.recipeImageUrl(recipe.getRecipeImageUrl())
					.build();
			})
			.toList();
	}
}
