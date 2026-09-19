package com.cookeep.cookeep.domain.cookeeps.application;

import com.cookeep.cookeep.api.dto.response.CookeepsFeedResponseDto;
import com.cookeep.cookeep.api.dto.response.CookeepsOnboardingResponseDto;
import com.cookeep.cookeep.api.dto.response.CookeepsRecipeDetailResponseDto;
import com.cookeep.cookeep.api.dto.response.RecipeRankingResponseDto;
import com.cookeep.cookeep.api.dto.response.RecipeRankingResponseDto.RecipeRankDto;
import com.cookeep.cookeep.api.dto.response.WateringRankingResponseDto;
import com.cookeep.cookeep.api.dto.response.WateringRankingResponseDto.WateringRankDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeBookmarkRepository;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeLikeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.plant.dao.WateringLogRepository;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CookeepsServiceTest {

    @Mock private UserReader userReader;
    @Mock private UserRepository userRepository;
    @Mock private WateringLogRepository wateringLogRepository;
    @Mock private DailyRecipeRepository dailyRecipeRepository;
    @Mock private RecipeLikeRepository recipeLikeRepository;
    @Mock private RecipeBookmarkRepository recipeBookmarkRepository;
    @Mock private RankingCacheService rankingCacheService;

    @InjectMocks
    private CookeepsService cookeepsService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        given(rankingCacheService.getWateringRanking(any(), any())).willReturn(List.of());
        given(wateringLogRepository.countByUserAndMonth(any(), any(), any())).willReturn(0L);
        given(rankingCacheService.getRecipeRanking(any(), any())).willReturn(List.of());
    }

    @Nested
    @DisplayName("getWateringRanking - 물주기 랭킹 월별 기준")
    class GetWateringRanking {

        @Test
        @DisplayName("물주기 랭킹 조회 시 이번 달 1일 00:00:00부터 다음 달 1일 00:00:00 범위로 조회한다")
        void 물주기_랭킹_이번달_범위로_조회() {
            cookeepsService.getWateringRanking(USER_ID);

            ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(rankingCacheService).getWateringRanking(startCaptor.capture(), endCaptor.capture());

            LocalDateTime expectedStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime expectedEnd = expectedStart.plusMonths(1);

            assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
            assertThat(endCaptor.getValue()).isEqualTo(expectedEnd);
        }

        @Test
        @DisplayName("rankingCacheService가 반환한 순위를 그대로 전달한다")
        void 물주기_랭킹_결과_그대로_전달() {
            List<WateringRankDto> ranking = List.of(
                    WateringRankDto.builder().rank(1).nickname("유저1").wateringCount(10L).build(),
                    WateringRankDto.builder().rank(2).nickname("유저2").wateringCount(7L).build(),
                    WateringRankDto.builder().rank(3).nickname("유저3").wateringCount(5L).build()
            );
            given(rankingCacheService.getWateringRanking(any(), any())).willReturn(ranking);

            WateringRankingResponseDto result = cookeepsService.getWateringRanking(USER_ID);

            assertThat(result.getWateringRanking()).isEqualTo(ranking);
        }

        @Test
        @DisplayName("이번 달 물주기 기록이 없으면 빈 리스트를 반환한다")
        void 물주기_없는경우_빈리스트_반환() {
            WateringRankingResponseDto result = cookeepsService.getWateringRanking(USER_ID);

            assertThat(result.getWateringRanking()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getWateringRanking - 나의 이번달 물주기 횟수")
    class MyWateringCount {

        @Test
        @DisplayName("나의 이번달 물주기 횟수를 반환한다")
        void 나의_이번달_물주기_횟수_반환() {
            given(wateringLogRepository.countByUserAndMonth(any(), any(), any())).willReturn(12L);

            WateringRankingResponseDto result = cookeepsService.getWateringRanking(USER_ID);

            assertThat(result.getMyWateringCount()).isEqualTo(12L);
        }

        @Test
        @DisplayName("나의 물주기 횟수 조회 시 이번 달 1일 00:00:00부터 다음 달 1일 00:00:00 범위와 userId로 조회한다")
        void 나의_물주기_횟수_이번달_범위_및_userId로_조회() {
            cookeepsService.getWateringRanking(USER_ID);

            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(wateringLogRepository).countByUserAndMonth(
                    userIdCaptor.capture(), startCaptor.capture(), endCaptor.capture());

            LocalDateTime expectedStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

            assertThat(userIdCaptor.getValue()).isEqualTo(USER_ID);
            assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
            assertThat(endCaptor.getValue()).isEqualTo(expectedStart.plusMonths(1));
        }

        @Test
        @DisplayName("이번 달 물주기 기록이 없으면 0을 반환한다")
        void 이번달_물주기_없으면_0_반환() {
            WateringRankingResponseDto result = cookeepsService.getWateringRanking(USER_ID);

            assertThat(result.getMyWateringCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("getRecipeRanking - 레시피 랭킹 주별 기준")
    class GetRecipeRanking {

        @Test
        @DisplayName("레시피 랭킹 조회 시 이번 주 월요일 00:00:00부터 정확히 7일 범위로 조회한다")
        void 레시피_랭킹_이번주_7일_범위로_조회() {
            cookeepsService.getRecipeRanking();

            ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(rankingCacheService).getRecipeRanking(startCaptor.capture(), endCaptor.capture());

            assertThat(endCaptor.getValue()).isEqualTo(startCaptor.getValue().plusDays(7));
        }

        @Test
        @DisplayName("rankingCacheService가 반환한 레시피 순위를 그대로 전달한다")
        void 레시피_랭킹_결과_그대로_전달() {
            List<RecipeRankDto> ranking = List.of(
                    RecipeRankDto.builder().dailyRecipeId(1L).rank(1).nickname("유저1")
                            .title("된장찌개").likeCount(10L).description("맛있어요").build()
            );
            given(rankingCacheService.getRecipeRanking(any(), any())).willReturn(ranking);

            RecipeRankingResponseDto result = cookeepsService.getRecipeRanking();

            assertThat(result.getRecipeRanking()).isEqualTo(ranking);
        }
    }

    @Nested
    @DisplayName("getOnboardingStatus - 쿠킵스 온보딩 상태 조회")
    class GetOnboardingStatus {

        @Test
        @DisplayName("온보딩 미완료 유저는 isCookeepsOnboarded가 false로 반환된다")
        void 온보딩_미완료_유저_false_반환() {
            User user = User.builder().nickname("유저").build();
            given(userReader.readById(USER_ID)).willReturn(user);

            CookeepsOnboardingResponseDto result = cookeepsService.getOnboardingStatus(USER_ID);

            assertThat(result.isCookeepsOnboarded()).isFalse();
        }

        @Test
        @DisplayName("온보딩 완료 유저는 isCookeepsOnboarded가 true로 반환된다")
        void 온보딩_완료_유저_true_반환() {
            User user = User.builder().nickname("유저").isCookeepsOnboarded(true).build();
            given(userReader.readById(USER_ID)).willReturn(user);

            CookeepsOnboardingResponseDto result = cookeepsService.getOnboardingStatus(USER_ID);

            assertThat(result.isCookeepsOnboarded()).isTrue();
        }
    }

    @Nested
    @DisplayName("confirmOnboarding - 쿠킵스 온보딩 완료 처리")
    class ConfirmOnboarding {

        @Test
        @DisplayName("온보딩 완료 처리 시 isCookeepsOnboarded가 true로 변경된다")
        void 온보딩_완료처리_후_true로_변경() {
            User user = User.builder().nickname("유저").build();
            given(userReader.readById(USER_ID)).willReturn(user);

            cookeepsService.confirmOnboarding(USER_ID);

            assertThat(user.isCookeepsOnboarded()).isTrue();
        }

        @Test
        @DisplayName("이미 온보딩 완료 상태여도 완료 처리 시 true로 유지된다")
        void 이미_완료상태에서_완료처리_true_유지() {
            User user = User.builder().nickname("유저").isCookeepsOnboarded(true).build();
            given(userReader.readById(USER_ID)).willReturn(user);

            cookeepsService.confirmOnboarding(USER_ID);

            assertThat(user.isCookeepsOnboarded()).isTrue();
        }
    }

    @Nested
    @DisplayName("getAllRecipes - 전체 공개 레시피 피드 조회")
    class GetAllRecipes {

        private User user;

        @BeforeEach
        void setUp() {
            user = User.builder().nickname("테스터").build();
            given(dailyRecipeRepository.findAllPublicRecipes(any(Pageable.class)))
                    .willReturn(new SliceImpl<>(List.of()));
        }

        private DailyRecipe buildRecipe(Long id, String title, int likeCount, String imageUrl, LocalDateTime createdAt) {
            DailyRecipe recipe = DailyRecipe.builder()
                    .id(id)
                    .title(title)
                    .content("{}")
                    .isPublic(true)
                    .likeCount(likeCount)
                    .recipeImageUrl(imageUrl)
                    .user(user)
                    .build();
            ReflectionTestUtils.setField(recipe, "createdAt", createdAt);
            return recipe;
        }

        @Test
        @DisplayName("filter가 latest이면 createdAt 내림차순 정렬로 조회한다")
        void filter_latest_createdAt_내림차순_조회() {
            cookeepsService.getAllRecipes("latest", PageRequest.of(0, 10));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(dailyRecipeRepository).findAllPublicRecipes(captor.capture());

            Sort.Order order = captor.getValue().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("filter가 likes이면 likeCount 내림차순, 동점 시 createdAt 내림차순 정렬로 조회한다")
        void filter_likes_likeCount_내림차순_createdAt_내림차순_조회() {
            cookeepsService.getAllRecipes("likes", PageRequest.of(0, 10));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(dailyRecipeRepository).findAllPublicRecipes(captor.capture());

            Sort sort = captor.getValue().getSort();
            assertThat(sort.getOrderFor("likeCount").getDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("filter가 oldest이면 createdAt 오름차순 정렬로 조회한다")
        void filter_oldest_createdAt_오름차순_조회() {
            cookeepsService.getAllRecipes("oldest", PageRequest.of(0, 10));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(dailyRecipeRepository).findAllPublicRecipes(captor.capture());

            Sort.Order order = captor.getValue().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("알 수 없는 filter 값은 최신순(createdAt 내림차순)으로 처리한다")
        void 알수없는_filter_최신순_처리() {
            cookeepsService.getAllRecipes("unknown", PageRequest.of(0, 10));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(dailyRecipeRepository).findAllPublicRecipes(captor.capture());

            Sort.Order order = captor.getValue().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("공개된 레시피가 없으면 빈 content를 반환한다")
        void 공개_레시피_없으면_빈_content_반환() {
            given(dailyRecipeRepository.findAllPublicRecipes(any(Pageable.class)))
                    .willReturn(new SliceImpl<>(List.of()));

            Slice<CookeepsFeedResponseDto> result = cookeepsService.getAllRecipes("latest", PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("조회된 레시피가 다음 페이지보다 적으면 hasNext가 false이다")
        void 마지막_페이지면_hasNext_false() {
            DailyRecipe recipe = buildRecipe(1L, "된장찌개", 5, null, LocalDateTime.now());
            given(dailyRecipeRepository.findAllPublicRecipes(any(Pageable.class)))
                    .willReturn(new SliceImpl<>(List.of(recipe), PageRequest.of(0, 10), false));

            Slice<CookeepsFeedResponseDto> result = cookeepsService.getAllRecipes("latest", PageRequest.of(0, 10));

            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext가 true이다")
        void 다음_페이지_있으면_hasNext_true() {
            DailyRecipe recipe = buildRecipe(1L, "된장찌개", 5, null, LocalDateTime.now());
            given(dailyRecipeRepository.findAllPublicRecipes(any(Pageable.class)))
                    .willReturn(new SliceImpl<>(List.of(recipe), PageRequest.of(0, 10), true));

            Slice<CookeepsFeedResponseDto> result = cookeepsService.getAllRecipes("latest", PageRequest.of(0, 10));

            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("조회된 레시피의 필드가 DTO에 올바르게 매핑된다")
        void 레시피_필드_DTO_올바르게_매핑() {
            LocalDateTime createdAt = LocalDateTime.of(2026, 3, 28, 14, 0, 0);
            DailyRecipe recipe = buildRecipe(42L, "된장찌개", 15, "https://example.com/img.jpg", createdAt);
            given(dailyRecipeRepository.findAllPublicRecipes(any(Pageable.class)))
                    .willReturn(new SliceImpl<>(List.of(recipe)));

            Slice<CookeepsFeedResponseDto> result = cookeepsService.getAllRecipes("latest", PageRequest.of(0, 10));

            CookeepsFeedResponseDto dto = result.getContent().get(0);
            assertThat(dto.getDailyRecipeId()).isEqualTo(42L);
            assertThat(dto.getNickname()).isEqualTo("테스터");
            assertThat(dto.getTitle()).isEqualTo("된장찌개");
            assertThat(dto.getLikeCount()).isEqualTo(15);
            assertThat(dto.getRecipeImageUrl()).isEqualTo("https://example.com/img.jpg");
            assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("recipeImageUrl이 없는 레시피는 null로 매핑된다")
        void recipeImageUrl_없으면_null_매핑() {
            DailyRecipe recipe = buildRecipe(1L, "바나나 쉐이크", 0, null, LocalDateTime.now());
            given(dailyRecipeRepository.findAllPublicRecipes(any(Pageable.class)))
                    .willReturn(new SliceImpl<>(List.of(recipe)));

            Slice<CookeepsFeedResponseDto> result = cookeepsService.getAllRecipes("latest", PageRequest.of(0, 10));

            assertThat(result.getContent().get(0).getRecipeImageUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("getCookeepsRecipeDetail - 쿠킵스 레시피 상세 조회 (viewer 상태)")
    class GetCookeepsRecipeDetail {

        private static final Long AUTHOR_ID = 10L;
        private static final Long RECIPE_ID = 100L;

        private User viewer;
        private User author;

        @BeforeEach
        void setUp() {
            viewer = User.builder().userId(USER_ID).nickname("조회자").build();
            author = User.builder().userId(AUTHOR_ID).nickname("작성자").build();
            given(userReader.readById(USER_ID)).willReturn(viewer);
        }

        private DailyRecipe buildRecipe(User writer, boolean isPublic) {
            return DailyRecipe.builder()
                    .id(RECIPE_ID)
                    .title("된장찌개")
                    .content("{}")
                    .isPublic(isPublic)
                    .likeCount(3)
                    .user(writer)
                    .build();
        }

        @Test
        @DisplayName("타인의 공개 레시피에 좋아요/북마크를 하지 않았다면 isLiked, isBookmarked는 false이고 canLike, canBookmark는 true이다")
        void 타인_레시피_상태없음() {
            DailyRecipe recipe = buildRecipe(author, true);
            given(dailyRecipeRepository.findById(RECIPE_ID)).willReturn(Optional.of(recipe));
            given(recipeLikeRepository.existsByDailyRecipeAndUser(recipe, viewer)).willReturn(false);
            given(recipeBookmarkRepository.existsByDailyRecipeAndUser(recipe, viewer)).willReturn(false);

            CookeepsRecipeDetailResponseDto dto = cookeepsService.getCookeepsRecipeDetail(RECIPE_ID, USER_ID);

            assertThat(dto.getIsLiked()).isFalse();
            assertThat(dto.getIsBookmarked()).isFalse();
            assertThat(dto.getCanLike()).isTrue();
            assertThat(dto.getCanBookmark()).isTrue();
        }

        @Test
        @DisplayName("타인의 공개 레시피에 이미 좋아요/북마크를 했다면 isLiked, isBookmarked는 true이고 canLike, canBookmark도 true이다")
        void 타인_레시피_이미_좋아요_북마크() {
            DailyRecipe recipe = buildRecipe(author, true);
            given(dailyRecipeRepository.findById(RECIPE_ID)).willReturn(Optional.of(recipe));
            given(recipeLikeRepository.existsByDailyRecipeAndUser(recipe, viewer)).willReturn(true);
            given(recipeBookmarkRepository.existsByDailyRecipeAndUser(recipe, viewer)).willReturn(true);

            CookeepsRecipeDetailResponseDto dto = cookeepsService.getCookeepsRecipeDetail(RECIPE_ID, USER_ID);

            assertThat(dto.getIsLiked()).isTrue();
            assertThat(dto.getIsBookmarked()).isTrue();
            assertThat(dto.getCanLike()).isTrue();
            assertThat(dto.getCanBookmark()).isTrue();
        }

        @Test
        @DisplayName("본인 레시피는 canLike, canBookmark가 false이다")
        void 본인_레시피는_누를_수_없다() {
            DailyRecipe recipe = buildRecipe(viewer, true);
            given(dailyRecipeRepository.findById(RECIPE_ID)).willReturn(Optional.of(recipe));

            CookeepsRecipeDetailResponseDto dto = cookeepsService.getCookeepsRecipeDetail(RECIPE_ID, USER_ID);

            assertThat(dto.getIsLiked()).isFalse();
            assertThat(dto.getIsBookmarked()).isFalse();
            assertThat(dto.getCanLike()).isFalse();
            assertThat(dto.getCanBookmark()).isFalse();
        }

        @Test
        @DisplayName("비공개 레시피는 DAILY_RECIPE_NOT_FOUND 예외가 발생한다")
        void 비공개_레시피_예외() {
            DailyRecipe recipe = buildRecipe(author, false);
            given(dailyRecipeRepository.findById(RECIPE_ID)).willReturn(Optional.of(recipe));

            assertThatThrownBy(() -> cookeepsService.getCookeepsRecipeDetail(RECIPE_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.DAILY_RECIPE_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 레시피는 DAILY_RECIPE_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_레시피_예외() {
            given(dailyRecipeRepository.findById(RECIPE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cookeepsService.getCookeepsRecipeDetail(RECIPE_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.DAILY_RECIPE_NOT_FOUND.getMessage());
        }
    }
}
