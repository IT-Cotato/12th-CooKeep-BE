package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
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
    @DisplayName("createDailyRecipe - PHOTO_RECORD 목표 연동")
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
        @DisplayName("사진 포함 등록 시 목표 달성이면 weeklyGoalAchieved=true를 반환한다")
        void 사진포함_등록_목표달성_true반환() {
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.PHOTO_RECORD)).willReturn(true);

            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.createDailyRecipe(1L, 10L, null, null, "https://s3.example.com/photo.jpg", false);

            assertThat(result.weeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("사진 없이 등록 시 weeklyGoalAchieved=false를 반환한다")
        void 사진없음_등록_weeklyGoalAchieved_false() {
            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.createDailyRecipe(1L, 10L, null, null, null, false);

            assertThat(result.weeklyGoalAchieved()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateDailyRecipe - PHOTO_RECORD 목표 연동")
    class UpdateDailyRecipe {

        private DailyRecipe existingRecipeWithoutPhoto;
        private DailyRecipe existingRecipeWithPhoto;

        @BeforeEach
        void setUp() {
            existingRecipeWithoutPhoto = DailyRecipe.builder()
                    .title("기존 레시피")
                    .content("{}")
                    .user(user)
                    .build();

            existingRecipeWithPhoto = DailyRecipe.builder()
                    .title("기존 레시피")
                    .content("{}")
                    .recipeImageUrl("https://s3.example.com/existing.jpg")
                    .user(user)
                    .build();
        }

        @Test
        @DisplayName("사진이 없던 레시피에 사진을 추가하면 PHOTO_RECORD 목표 진행을 호출한다")
        void 신규사진_추가_목표진행_호출() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(existingRecipeWithoutPhoto));

            dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/new.jpg", false);

            verify(weeklyGoalService).handleGoalProgress(1L, GoalActionType.PHOTO_RECORD);
        }

        @Test
        @DisplayName("이미 사진이 있는 레시피에 사진을 교체하면 PHOTO_RECORD 목표 진행을 호출하지 않는다")
        void 기존사진_교체_목표진행_미호출() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(existingRecipeWithPhoto));

            dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/replaced.jpg", false);

            verify(weeklyGoalService, never()).handleGoalProgress(any(), any());
        }

        @Test
        @DisplayName("신규 사진 추가 시 목표 달성이면 weeklyGoalAchieved=true를 반환한다")
        void 신규사진_추가_목표달성_true반환() {
            given(dailyRecipeRepository.findByIdAndUser(100L, user))
                    .willReturn(Optional.of(existingRecipeWithoutPhoto));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.PHOTO_RECORD)).willReturn(true);

            DailyRecipeService.DailyRecipeResult result =
                    dailyRecipeService.updateDailyRecipe(1L, 100L, null, null, "https://s3.example.com/new.jpg", false);

            assertThat(result.weeklyGoalAchieved()).isTrue();
        }
    }
}
