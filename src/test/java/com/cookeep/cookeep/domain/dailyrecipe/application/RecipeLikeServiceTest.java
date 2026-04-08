package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.api.dto.response.CookeepsFeedResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.dao.RecipeLikeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.dailyrecipe.entity.RecipeLike;
import com.cookeep.cookeep.domain.onboarding.application.WeeklyGoalService;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeLikeServiceTest {

    @Mock
    private RecipeLikeRepository recipeLikeRepository;

    @Mock
    private DailyRecipeRepository dailyRecipeRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private WeeklyGoalService weeklyGoalService;

    @InjectMocks
    private RecipeLikeService recipeLikeService;

    private User liker;
    private User recipeOwner;
    private DailyRecipe recipe;

    @BeforeEach
    void setUp() {
        liker = User.builder().nickname("좋아요누르는유저").build();
        recipeOwner = User.builder().nickname("레시피작성자").build();

        // 리플렉션으로 userId 설정 없이 테스트하기 위해 별도 User 객체 사용
        // recipeOwner의 userId != liker의 userId 를 보장하기 위해 mock 사용
        liker = mock(User.class);
        recipeOwner = mock(User.class);
        given(liker.getUserId()).willReturn(1L);
        given(recipeOwner.getUserId()).willReturn(2L);

        recipe = DailyRecipe.builder()
                .title("테스트 레시피")
                .content("{}")
                .isPublic(true)
                .user(recipeOwner)
                .build();
    }

    @Nested
    @DisplayName("toggleLike - 좋아요 추가")
    class LikeAdd {

        @BeforeEach
        void setUp() {
            given(userReader.readById(1L)).willReturn(liker);
            given(dailyRecipeRepository.findById(100L)).willReturn(Optional.of(recipe));
            given(recipeLikeRepository.findByDailyRecipeAndUser(recipe, liker)).willReturn(Optional.empty());
            given(recipeLikeRepository.save(any(RecipeLike.class))).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("좋아요 추가 시 isLiked=true를 반환한다")
        void 좋아요추가_isLiked_true() {
            RecipeLikeService.ToggleLikeResult result = recipeLikeService.toggleLike(1L, 100L);

            assertThat(result.isLiked()).isTrue();
        }

        @Test
        @DisplayName("좋아요 추가 시 주간 목표 진행을 호출한다")
        void 좋아요추가_목표진행_호출() {
            recipeLikeService.toggleLike(1L, 100L);

            verify(weeklyGoalService).handleGoalProgress(1L, GoalActionType.RECIPE_LIKE);
        }

        @Test
        @DisplayName("좋아요 추가 시 목표 달성이면 weeklyGoalAchieved=true를 반환한다")
        void 좋아요추가_목표달성_true반환() {
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE)).willReturn(true);

            RecipeLikeService.ToggleLikeResult result = recipeLikeService.toggleLike(1L, 100L);

            assertThat(result.weeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("좋아요 추가 시 목표 미달성이면 weeklyGoalAchieved=false를 반환한다")
        void 좋아요추가_목표미달성_false반환() {
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE)).willReturn(false);

            RecipeLikeService.ToggleLikeResult result = recipeLikeService.toggleLike(1L, 100L);

            assertThat(result.weeklyGoalAchieved()).isFalse();
        }
    }

    @Nested
    @DisplayName("toggleLike - 좋아요 취소")
    class LikeCancel {

        private RecipeLike existingLike;

        @BeforeEach
        void setUp() {
            existingLike = RecipeLike.builder().dailyRecipe(recipe).user(liker).build();

            given(userReader.readById(1L)).willReturn(liker);
            given(dailyRecipeRepository.findById(100L)).willReturn(Optional.of(recipe));
            given(recipeLikeRepository.findByDailyRecipeAndUser(recipe, liker))
                    .willReturn(Optional.of(existingLike));
        }

        @Test
        @DisplayName("좋아요 취소 시 isLiked=false를 반환한다")
        void 좋아요취소_isLiked_false() {
            RecipeLikeService.ToggleLikeResult result = recipeLikeService.toggleLike(1L, 100L);

            assertThat(result.isLiked()).isFalse();
        }

        @Test
        @DisplayName("좋아요 취소 시 목표 카운트 감소를 호출한다")
        void 좋아요취소_목표되돌리기_호출() {
            recipeLikeService.toggleLike(1L, 100L);

            verify(weeklyGoalService).handleGoalUndo(1L, GoalActionType.RECIPE_LIKE);
        }

        @Test
        @DisplayName("좋아요 취소 시 weeklyGoalAchieved=false를 반환한다")
        void 좋아요취소_weeklyGoalAchieved_false() {
            RecipeLikeService.ToggleLikeResult result = recipeLikeService.toggleLike(1L, 100L);

            assertThat(result.weeklyGoalAchieved()).isFalse();
        }

        @Test
        @DisplayName("좋아요 취소 시 handleGoalProgress를 호출하지 않는다")
        void 좋아요취소_목표진행_미호출() {
            recipeLikeService.toggleLike(1L, 100L);

            verify(weeklyGoalService, never()).handleGoalProgress(any(), any());
        }
    }

    @Nested
    @DisplayName("toggleLike - 예외 케이스")
    class LikeException {

        @Test
        @DisplayName("자신의 레시피에 좋아요를 누르면 예외가 발생한다")
        void 자신의레시피_좋아요_예외() {
            given(userReader.readById(2L)).willReturn(recipeOwner);
            given(dailyRecipeRepository.findById(100L)).willReturn(Optional.of(recipe));

            assertThatThrownBy(() -> recipeLikeService.toggleLike(2L, 100L))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_LIKE_OWN_RECIPE.getMessage());

            verifyNoInteractions(weeklyGoalService);
        }

        @Test
        @DisplayName("존재하지 않는 레시피에 좋아요를 누르면 예외가 발생한다")
        void 없는레시피_좋아요_예외() {
            given(userReader.readById(1L)).willReturn(liker);
            given(dailyRecipeRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeLikeService.toggleLike(1L, 999L))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.DAILY_RECIPE_NOT_FOUND.getMessage());

            verifyNoInteractions(weeklyGoalService);
        }
    }

    @Nested
    @DisplayName("getMyLikedRecipes - 내가 좋아요 누른 레시피 목록 조회")
    class GetMyLikedRecipes {

        private DailyRecipe likedRecipe;
        private Pageable pageable;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 10);
            likedRecipe = DailyRecipe.builder()
                    .title("좋아요한 레시피")
                    .content("{}")
                    .isPublic(true)
                    .likeCount(5)
                    .recipeImageUrl("http://image.url")
                    .user(recipeOwner)
                    .build();
            ReflectionTestUtils.setField(likedRecipe, "id", 10L);
            ReflectionTestUtils.setField(likedRecipe, "createdAt", LocalDateTime.of(2024, 1, 1, 12, 0));

            given(userReader.readById(1L)).willReturn(liker);
        }

        @Test
        @DisplayName("레시피 목록을 CookeepsFeedResponseDto의 필드에 올바르게 매핑한다")
        void 목록조회_DTO_필드_매핑() {
            Slice<DailyRecipe> slice = new SliceImpl<>(List.of(likedRecipe), pageable, false);
            given(recipeLikeRepository.findMyLikedRecipes(liker, pageable)).willReturn(slice);

            Slice<CookeepsFeedResponseDto> result = recipeLikeService.getMyLikedRecipes(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            CookeepsFeedResponseDto dto = result.getContent().get(0);
            assertThat(dto.getDailyRecipeId()).isEqualTo(10L);
            assertThat(dto.getTitle()).isEqualTo("좋아요한 레시피");
            assertThat(dto.getLikeCount()).isEqualTo(5);
            assertThat(dto.getRecipeImageUrl()).isEqualTo("http://image.url");
            assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
        }

        @Test
        @DisplayName("다음 페이지가 있으면 last=false를 반환한다")
        void 다음페이지있으면_last_false() {
            Slice<DailyRecipe> slice = new SliceImpl<>(List.of(likedRecipe), pageable, true);
            given(recipeLikeRepository.findMyLikedRecipes(liker, pageable)).willReturn(slice);

            Slice<CookeepsFeedResponseDto> result = recipeLikeService.getMyLikedRecipes(1L, pageable);

            assertThat(result.isLast()).isFalse();
        }

        @Test
        @DisplayName("마지막 페이지이면 last=true를 반환한다")
        void 마지막페이지이면_last_true() {
            Slice<DailyRecipe> slice = new SliceImpl<>(List.of(likedRecipe), pageable, false);
            given(recipeLikeRepository.findMyLikedRecipes(liker, pageable)).willReturn(slice);

            Slice<CookeepsFeedResponseDto> result = recipeLikeService.getMyLikedRecipes(1L, pageable);

            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("좋아요한 레시피가 없으면 빈 Slice를 반환한다")
        void 좋아요레시피없으면_빈Slice() {
            Slice<DailyRecipe> emptySlice = new SliceImpl<>(List.of(), pageable, false);
            given(recipeLikeRepository.findMyLikedRecipes(liker, pageable)).willReturn(emptySlice);

            Slice<CookeepsFeedResponseDto> result = recipeLikeService.getMyLikedRecipes(1L, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.isLast()).isTrue();
        }
    }
}
