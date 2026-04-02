package com.cookeep.cookeep.domain.cookeeps.application;

import com.cookeep.cookeep.api.dto.response.CookeepsOnboardingResponseDto;
import com.cookeep.cookeep.api.dto.response.CookeepsRecipeDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.RecipeRankDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.WateringRankDto;
import com.cookeep.cookeep.api.dto.response.CookeepsFeedResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.plant.dao.WateringLogRepository;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

	private final UserReader userReader;
	private final WateringLogRepository wateringLogRepository;
	private final DailyRecipeRepository dailyRecipeRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public RankingResponseDto getRanking(Long userId) {
		LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
		LocalDateTime monthEnd = monthStart.plusMonths(1);

		LocalDateTime weekStart = LocalDate.now()
			.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			.atStartOfDay();
		LocalDateTime weekEnd = weekStart.plusDays(7);

		List<WateringRankDto> wateringRanking = getWateringRanking(monthStart, monthEnd);
		List<RecipeRankDto> recipeRanking = getRecipeRanking(weekStart, weekEnd);
		Long myWateringCount = wateringLogRepository.countByUserAndMonth(userId, monthStart, monthEnd);

		return RankingResponseDto.builder()
			.wateringRanking(wateringRanking)
			.recipeRanking(recipeRanking)
			.myWateringCount(myWateringCount)
			.build();
	}

	private List<WateringRankDto> getWateringRanking(LocalDateTime monthStart, LocalDateTime monthEnd) {
		// 1단계: 이번 달 물주기 상위 3명 조회
		List<Object[]> results = wateringLogRepository.findTopWateringUsers(
			monthStart, monthEnd, PageRequest.of(0, 3));
		
		// 2단계: 인덱스를 포함한 스트림 처리
		return IntStream.range(0, results.size()) // 0부터 results 크기-1까지
			.mapToObj(index -> {
				Object[] row = results.get(index);
				User user = (User) row[0]; // 유저 객체 추출
				Long wateringCount = (Long) row[1]; // 물주기 횟수 추출

				// 프로필 이미지 URL 가져오기
				String profileImageUrl = user.getProfilePlant() != null
					? user.getProfilePlant().getCurrentImageUrl()
					: null;

				// DTO로 변환 (rank = 인덱스 + 1 → 1, 2, 3)
				return WateringRankDto.builder()
					.rank(index + 1)
					.nickname(user.getNickname())
					.profileImageUrl(profileImageUrl)
					.wateringCount(wateringCount)
					.build();
			})
			.toList();
	}

	private List<RecipeRankDto> getRecipeRanking(LocalDateTime weekStart, LocalDateTime weekEnd) {
		// 이번 주 공개 레시피 중 좋아요 상위 3개 조회 (좋아요 내림차순, 동점 시 등록 오래된 순)
		List<DailyRecipe> results = dailyRecipeRepository.findTopRankedRecipes(
			weekStart, weekEnd, PageRequest.of(0, 3));

		return IntStream.range(0, results.size())
			.mapToObj(index -> {
				DailyRecipe recipe = results.get(index);

				return RecipeRankDto.builder()
						.dailyRecipeId(recipe.getId())
						.rank(index + 1)
						.title(recipe.getTitle())
						.likeCount(recipe.getLikeCount().longValue())
						.recipeImageUrl(recipe.getRecipeImageUrl())
						.build();
			})
			.toList();
	}

	@Transactional(readOnly = true)
	public Slice<CookeepsFeedResponseDto> getAllRecipes(String filter, Pageable pageable) {
		//  1단계: 정렬 기준 결정 (switch)
		Sort sort = switch (filter) {
			case "likes" -> Sort.by(Sort.Direction.DESC, "likeCount")
					.and(Sort.by(Sort.Direction.DESC, "createdAt"));
			case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
			default -> Sort.by(Sort.Direction.DESC, "createdAt"); // 기본: 최신순
		};

		// 2단계: 정렬 포함된 새 Pageable 생성
		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

		// 3단계: DB 조회 - Slice이므로 COUNT 쿼리 없이 size+1개만 가져옴
		Slice<DailyRecipe> recipes = dailyRecipeRepository.findAllPublicRecipes(sortedPageable);

		return recipes.map(recipe -> CookeepsFeedResponseDto.builder()
				.dailyRecipeId(recipe.getId())
				.title(recipe.getTitle())
				.likeCount(recipe.getLikeCount())
				.recipeImageUrl(recipe.getRecipeImageUrl())
				.createdAt(recipe.getCreatedAt())
				.build());
	}

	@Transactional(readOnly = true)
	public CookeepsOnboardingResponseDto getOnboardingStatus(Long userId) {
		User user = userReader.readById(userId);

		return CookeepsOnboardingResponseDto.builder()
			.isCookeepsOnboarded(user.isCookeepsOnboarded())
			.build();
	}

	@Transactional
	public void confirmOnboarding(Long userId) {
		User user = userReader.readById(userId);
		user.confirmCookeepsOnboarding();
	}

	@Transactional(readOnly = true)
	public CookeepsRecipeDetailResponseDto getCookeepsRecipeDetail(Long dailyRecipeId) {
		DailyRecipe dailyRecipe = dailyRecipeRepository.findById(dailyRecipeId)
				.orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));

		// 공개되지 않은 레시피는 커뮤니티 상세 조회가 불가능하도록 방어 로직
		if (!dailyRecipe.getIsPublic()) {
			throw new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND); // 혹은 권한 에러
		}

		return CookeepsRecipeDetailResponseDto.from(dailyRecipe);
	}
}
