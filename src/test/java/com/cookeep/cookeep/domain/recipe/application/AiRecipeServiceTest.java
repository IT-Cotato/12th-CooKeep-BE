package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.api.dto.response.AiRecipeAdoptResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.application.ConsumptionReportService;
import com.cookeep.cookeep.domain.onboarding.application.WeeklyGoalService;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import com.cookeep.cookeep.domain.recipe.dao.AiMessageRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiRecipeRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiSessionRepository;
import com.cookeep.cookeep.domain.recipe.entity.*;
import com.cookeep.cookeep.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AiRecipeServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private AiSessionRepository aiSessionRepository;
    @Mock private AiMessageRepository aiMessageRepository;
    @Mock private AiRecipeRepository aiRecipeRepository;
    @Mock private UserIngredientRepository userIngredientRepository;
    @Mock private DefaultIngredientRepository defaultIngredientRepository;
    @Mock private CustomIngredientRepository customIngredientRepository;
    @Mock private CookieService cookieService;
    @Mock private YoutubeSearchService youtubeSearchService;
    @Mock private ConsumptionReportService consumptionReportService;
    @Mock private DailyRecipeRepository dailyRecipeRepository;
    @Mock private WeeklyGoalService weeklyGoalService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiRecipeService aiRecipeService;

    private AiSession session;
    private AiMessage lastAiMessage;
    private AiRecipe savedAiRecipe;
    private User user;

    // 테스트용 최소 AI 응답 JSON
    private static final String VALID_AI_RESPONSE_JSON = """
            {
              "title": "테스트 레시피",
              "ingredients": {
                "user_ingredients": [
                  {"ingredientId": 1, "name": "양파", "quantity": 1.0, "unit": "개"}
                ]
              },
              "steps": ["1. 양파를 썬다", "2. 볶는다"],
              "youtube_search_queries": ["양파볶음 만들기"]
            }
            """;

    @BeforeEach
    void setUp() throws Exception {
        user = User.builder().nickname("테스터").build();

        session = AiSession.builder()
                .userId(1L)
                .difficulty(Difficulty.EASY)
                .attemptNumber(1)
                .isCompleted(false)
                .userIngredientIds("[]")
                .ingredientIdsJson("[1]")
                .build();

        lastAiMessage = AiMessage.builder()
                .session(session)
                .role(Role.AI)
                .messageType(MessageType.INITIAL_REQUEST)
                .content(VALID_AI_RESPONSE_JSON)
                .build();

        savedAiRecipe = AiRecipe.builder()
                .id(99L)
                .title("테스트 레시피")
                .session(session)
                .userId(1L)
                .ingredientsJson("[]")
                .stepsJson("[]")
                .build();

        given(aiSessionRepository.findByIdAndUserId(anyLong(), eq(1L)))
                .willReturn(Optional.of(session));
        given(aiMessageRepository.findTopBySessionAndRoleOrderByCreatedAtDesc(any(), eq(Role.AI)))
                .willReturn(Optional.of(lastAiMessage));
        given(aiRecipeRepository.save(any(AiRecipe.class))).willReturn(savedAiRecipe);
        given(aiMessageRepository.save(any(AiMessage.class))).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(aiMessageRepository).flush();
        given(dailyRecipeRepository.existsByAiRecipe_Session_Id(anyLong())).willReturn(false);

        // 기본값: 모든 목표 진행 false (미달성)
        given(weeklyGoalService.handleGoalProgress(anyLong(), any())).willReturn(false);
        // 기본값: 쿠키 지급 false
        given(cookieService.grantDailyCookie(anyLong(), any())).willReturn(false);
    }

    // 유통기한 임박(leftDays=0) 재료 생성 헬퍼
    private UserIngredient buildUrgentIngredient() {
        return spy(UserIngredient.builder()
                .type(Type.DEFAULT)
                .referenceId(1L)
                .quantity(1)
                .unit(Unit.PIECE)
                .storage(Storage.FRIDGE)
                .expirationDate(LocalDate.now()) // today → leftDays=0
                .user(user)
                .build());
    }

    // 유통기한 여유 있는 재료 생성 헬퍼
    private UserIngredient buildNormalIngredient() {
        return spy(UserIngredient.builder()
                .type(Type.DEFAULT)
                .referenceId(1L)
                .quantity(2)
                .unit(Unit.PIECE)
                .storage(Storage.FRIDGE)
                .expirationDate(LocalDate.now().plusDays(7)) // +7일
                .user(user)
                .build());
    }

    @Nested
    @DisplayName("adoptRecipe - COOKING 주간 목표 연동")
    class AdoptRecipeCookingGoal {

        @Test
        @DisplayName("레시피 채택 시 COOKING 목표 진행을 반드시 1회 호출한다")
        void 채택시_COOKING_목표_1회_호출() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildNormalIngredient()));

            aiRecipeService.adoptRecipe(1L, 10L);

            verify(weeklyGoalService, times(1)).handleGoalProgress(1L, GoalActionType.COOKING);
        }

        @Test
        @DisplayName("COOKING 목표 달성 시 weeklyGoalAchieved=true를 반환한다")
        void COOKING_달성_true() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildNormalIngredient()));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(true);

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("COOKING 미달성 + 임박 재료 없으면 weeklyGoalAchieved=false를 반환한다")
        void COOKING_미달성_임박없음_false() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildNormalIngredient()));
            // weeklyGoalService 기본값이 false이므로 별도 stubbing 불필요

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isFalse();
        }

        @Test
        @DisplayName("이미 완료된 세션 채택 시 예외가 발생하고 목표 진행을 호출하지 않는다")
        void 완료된세션_예외_목표_미호출() {
            session.complete();

            try {
                aiRecipeService.adoptRecipe(1L, 10L);
            } catch (AppException ignored) {}

            verify(weeklyGoalService, never()).handleGoalProgress(any(), any());
        }
    }

    @Nested
    @DisplayName("adoptRecipe - USE_EXPIRING_INGREDIENT 목표 연동")
    class ExpiringIngredientGoal {

        @Test
        @DisplayName("임박 재료 1개 포함 채택 시 USE_EXPIRING_INGREDIENT 목표 진행을 1회 호출한다")
        void 임박재료_1개_채택_목표_1회_호출() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildUrgentIngredient()));

            aiRecipeService.adoptRecipe(1L, 10L);

            verify(weeklyGoalService, times(1))
                    .handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);
        }

        @Test
        @DisplayName("임박 재료 없는 채택 시 USE_EXPIRING_INGREDIENT 목표 진행을 호출하지 않는다")
        void 임박재료_없음_채택_목표_미호출() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildNormalIngredient()));

            aiRecipeService.adoptRecipe(1L, 10L);

            verify(weeklyGoalService, never())
                    .handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);
        }

        @Test
        @DisplayName("임박 재료 포함 채택으로 USE_EXPIRING 달성 시 weeklyGoalAchieved=true를 반환한다")
        void 임박재료_USE_EXPIRING_달성_true() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildUrgentIngredient()));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(false);
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT)).willReturn(true);

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("임박 재료 있지만 USE_EXPIRING 미달성 + COOKING 미달성이면 weeklyGoalAchieved=false를 반환한다")
        void 임박재료_USE_EXPIRING_미달성_COOKING_미달성_false() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildUrgentIngredient()));

            // 기본값이 false이므로 별도 stubbing 불필요
            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isFalse();
        }
    }

    @Nested
    @DisplayName("adoptRecipe - COOKING + USE_EXPIRING_INGREDIENT 조합")
    class BothGoals {

        @Test
        @DisplayName("COOKING만 달성 시 weeklyGoalAchieved=true를 반환한다")
        void COOKING만_달성_true() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildNormalIngredient()));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(true);

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("USE_EXPIRING만 달성 시 weeklyGoalAchieved=true를 반환한다")
        void USE_EXPIRING만_달성_true() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildUrgentIngredient()));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(false);
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT)).willReturn(true);

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("COOKING과 USE_EXPIRING 모두 달성 시 weeklyGoalAchieved=true를 반환한다")
        void 둘다_달성_true() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildUrgentIngredient()));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(true);
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT)).willReturn(true);

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("COOKING과 USE_EXPIRING 모두 미달성 시 weeklyGoalAchieved=false를 반환한다")
        void 둘다_미달성_false() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(buildNormalIngredient()));
            // 기본값이 false이므로 별도 stubbing 불필요

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isFalse();
        }
    }

}
