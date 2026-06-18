package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;

import static org.mockito.ArgumentMatchers.anyLong;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.onboarding.application.WeeklyGoalService;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import com.cookeep.cookeep.domain.recipe.dao.AiRecipeRepository;
import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.common.util.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DailyRecipeServiceTest {

    @Mock private DailyRecipeRepository dailyRecipeRepository;
    @Mock private AiRecipeRepository aiRecipeRepository;
    @Mock private CookieService cookieService;
    @Mock private WeeklyGoalService weeklyGoalService;
    @Mock private UserReader userReader;
    @Mock private ObjectMapper objectMapper;
    @Mock private S3Service s3Service;

    @InjectMocks
    private DailyRecipeService dailyRecipeService;

    private User user;
    private AiRecipe aiRecipe;

    @BeforeEach
    void setUp() throws Exception {
        user = User.builder().nickname("테스터").build();
        aiRecipe = AiRecipe.builder()
                .id(10L)
                .userId(1L)
                .title("테스트 레시피")
                .ingredientsJson("[]")
                .stepsJson("[]")
                .build();

        given(userReader.readById(1L)).willReturn(user);
        given(aiRecipeRepository.findById(10L)).willReturn(Optional.of(aiRecipe));
        given(dailyRecipeRepository.save(any(DailyRecipe.class))).willAnswer(inv -> inv.getArgument(0));

        // ObjectMapper는 buildContentSnapshot() 내부에서 사용 — 실제 인스턴스로 교체
        ObjectMapper realMapper = new ObjectMapper();
        given(objectMapper.readTree("[]")).willReturn(realMapper.readTree("[]"));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
    }

    @Nested
    @DisplayName("createDailyRecipe")
    class CreateDailyRecipe {

        @Test
        @DisplayName("사진 포함 등록 시 PHOTO_RECORD 목표 진행을 호출한다")
        void 사진포함_등록_목표진행_호출() {
            dailyRecipeService.createDailyRecipe(1L, 10L, null, null, "https://s3.example.com/photo.jpg", false);

            verify(weeklyGoalService).handleGoalProgress(1L, GoalActionType.PHOTO_RECORD);
        }

        @Test
        @DisplayName("사진 없이 등록 시 PHOTO_RECORD 목표 진행을 호출하지 않는다")
        void 사진없음_등록_목표진행_미호출() {
            dailyRecipeService.createDailyRecipe(1L, 10L, null, null, null, false);

            verify(weeklyGoalService, never()).handleGoalProgress(any(), any());
        }

        @Test
        @DisplayName("사진 포함 등록 시 목표 달성이면 reward.types에 BONUS_WEEKLY_GOAL_ACHIEVE가 포함된다")
        void 사진포함_등록_목표달성_true반환() {
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.PHOTO_RECORD)).willReturn(true);

            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.createDailyRecipe(1L, 10L, null, null, "https://s3.example.com/photo.jpg", false);

            assertThat(result.reward().getTypes())
                    .contains(CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }

        @Test
        @DisplayName("사진 없이 등록 시 reward.types에 BONUS_WEEKLY_GOAL_ACHIEVE가 없다")
        void 사진없음_등록_weeklyGoalAchieved_false() {
            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.createDailyRecipe(1L, 10L, null, null, null, false);

            assertThat(result.reward().getTypes())
                    .doesNotContain(CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }

        @Test
        @DisplayName("사진 포함 등록 시 reward.types에 BASIC_FOOD_PHOTO_REG가 포함된다")
        void 사진포함_등록_photoCookieAwarded_true() {
            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.createDailyRecipe(1L, 10L, null, null, "https://s3.example.com/photo.jpg", false);

            assertThat(result.reward().getTypes())
                    .contains(CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }

        @Test
        @DisplayName("사진 없이 등록 시 reward.types에 BASIC_FOOD_PHOTO_REG가 없다")
        void 사진없음_등록_photoCookieAwarded_false() {
            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.createDailyRecipe(1L, 10L, null, null, null, false);

            assertThat(result.reward().getTypes())
                    .doesNotContain(CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }

        @Test
        @DisplayName("사진 포함 등록 시 BASIC_FOOD_PHOTO_REG 쿠키를 지급한다")
        void 사진포함_등록_사진쿠키_지급() {
            dailyRecipeService.createDailyRecipe(1L, 10L, null, null, "https://s3.example.com/photo.jpg", false);

            verify(cookieService).updateCookie(1L, CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }

        @Test
        @DisplayName("사진 없이 등록 시 BASIC_FOOD_PHOTO_REG 쿠키를 지급하지 않는다")
        void 사진없음_등록_사진쿠키_미지급() {
            dailyRecipeService.createDailyRecipe(1L, 10L, null, null, null, false);

            verify(cookieService, never()).updateCookie(1L, CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }
    }

    @Nested
    @DisplayName("updateDailyRecipe")
    class UpdateDailyRecipe {

        // 사진 보상 미완료 상태 (처음 사진 추가 가능)
        private DailyRecipe rewardPendingRecipe;
        // 사진 보상 완료 상태 (재지급 불가)
        private DailyRecipe rewardCompletedRecipe;

        @BeforeEach
        void setUp() {
            rewardPendingRecipe = DailyRecipe.builder()
                    .title("기존 레시피")
                    .content("{}")
                    .user(user)
                    .photoRewardCompleted(false)
                    .build();

            rewardCompletedRecipe = DailyRecipe.builder()
                    .title("기존 레시피")
                    .content("{}")
                    .recipeImageUrl("https://s3.example.com/existing.jpg")
                    .photoRewardCompleted(true)
                    .user(user)
                    .build();
        }

        @Test
        @DisplayName("보상 미완료 레시피에 처음 사진 추가 시 PHOTO_RECORD 목표 진행을 호출한다")
        void 신규사진_추가_목표진행_호출() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardPendingRecipe));

            dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/new.jpg", false);

            verify(weeklyGoalService).handleGoalProgress(1L, GoalActionType.PHOTO_RECORD);
        }

        @Test
        @DisplayName("보상 완료된 레시피에 사진 교체 시 PHOTO_RECORD 목표 진행을 호출하지 않는다")
        void 보상완료_사진교체_목표진행_미호출() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardCompletedRecipe));

            dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/replaced.jpg", false);

            verify(weeklyGoalService, never()).handleGoalProgress(any(), any());
        }

        @Test
        @DisplayName("보상 미완료 레시피에 처음 사진 추가 시 reward.types에 BASIC_FOOD_PHOTO_REG가 포함된다")
        void 신규사진_추가_photoCookieAwarded_true() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardPendingRecipe));

            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/new.jpg", false);

            assertThat(result.reward().getTypes())
                    .contains(CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }

        @Test
        @DisplayName("보상 완료된 레시피에 사진 교체 시 reward.types에 BASIC_FOOD_PHOTO_REG가 없다")
        void 보상완료_사진교체_photoCookieAwarded_false() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardCompletedRecipe));

            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/replaced.jpg", false);

            assertThat(result.reward().getTypes())
                    .doesNotContain(CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }

        @Test
        @DisplayName("보상 미완료 레시피에 처음 사진 추가 시 BASIC_FOOD_PHOTO_REG 쿠키를 지급한다")
        void 신규사진_추가_사진쿠키_지급() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardPendingRecipe));

            dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/new.jpg", false);

            verify(cookieService).updateCookie(1L, CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }

        @Test
        @DisplayName("보상 완료된 레시피에 사진 교체 시 BASIC_FOOD_PHOTO_REG 쿠키를 지급하지 않는다")
        void 보상완료_사진교체_사진쿠키_미지급() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardCompletedRecipe));

            dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/replaced.jpg", false);

            verify(cookieService, never()).updateCookie(1L, CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
        }

        @Test
        @DisplayName("신규 사진 추가 시 목표 달성이면 reward.types에 BONUS_WEEKLY_GOAL_ACHIEVE가 포함된다")
        void 신규사진_추가_목표달성_true반환() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardPendingRecipe));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.PHOTO_RECORD)).willReturn(true);

            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/new.jpg", false);

            assertThat(result.reward().getTypes())
                    .contains(CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }

        @Test
        @DisplayName("사진 삭제 후 재등록 시 쿠키와 목표 진행이 중복 지급되지 않는다 (어뷰징 방지)")
        void 사진삭제후재등록_중복지급_방지() {
            // 이미 사진 보상을 받은 레시피에서 사진을 삭제한 상태 (photoRewardCompleted = true, recipeImageUrl = null)
            DailyRecipe rewardedButNoPhoto = DailyRecipe.builder()
                    .title("기존 레시피")
                    .content("{}")
                    .recipeImageUrl(null)
                    .photoRewardCompleted(true)
                    .user(user)
                    .build();
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(rewardedButNoPhoto));

            dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/re-uploaded.jpg", false);

            verify(cookieService, never()).updateCookie(1L, CookieLog.CookieLogType.BASIC_FOOD_PHOTO_REG);
            verify(weeklyGoalService, never()).handleGoalProgress(any(), any());
        }
    }
}
