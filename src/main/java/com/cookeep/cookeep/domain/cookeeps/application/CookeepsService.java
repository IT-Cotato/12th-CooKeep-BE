package com.cookeep.cookeep.domain.cookeeps.application;

import com.cookeep.cookeep.api.dto.response.CookeepsOnboardingResponseDto;
import com.cookeep.cookeep.api.dto.response.CookeepsRecipeDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.RecipeRankDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto.WateringRankDto;
import com.cookeep.cookeep.api.dto.response.WeeklyRecipeResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.common.util.DateTimeUtils;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;

import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.plant.dao.WateringLogRepository;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
	public Page<WeeklyRecipeResponseDto> getWeeklyRecipes(String filter, Pageable pageable) {
		// 1. 유틸리티를 사용해 이번 주 월요일 00:00:00 가져오기
		LocalDateTime start = DateTimeUtils.getStartOfWeek();
		LocalDateTime end = start.plusDays(7); // 다음 주 월요일 00:00:00 이전까지

		// 2. 필터에 따른 정렬 기준 동적 설정
		Sort sort = switch (filter) {
			case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
			case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
			default -> Sort.by(Sort.Direction.DESC, "likeCount")
					.and(Sort.by(Sort.Direction.DESC, "createdAt"));
		};

		// 3. 정렬이 포함된 새로운 Pageable 생성
		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

		// 4. 데이터 조회 및 DTO 변환
		Page<DailyRecipe> recipes = dailyRecipeRepository.findWeeklyPublicRecipes(start, end, sortedPageable);

		// 5. 순위(rank) 계산을 포함하여 변환
		int startRank = (int) sortedPageable.getOffset() + 1;
		return recipes.map(recipe -> {
			int currentIndex = recipes.getContent().indexOf(recipe);
			return WeeklyRecipeResponseDto.builder()
					.rank(startRank + currentIndex)
					.dailyRecipeId(recipe.getId())
					.title(recipe.getTitle())
					.likeCount(recipe.getLikeCount())
					.recipeImageUrl(recipe.getRecipeImageUrl())
					.build();
		});
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
