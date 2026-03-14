package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.api.dto.response.AiRecipeAdoptResponseDto;
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

    private AiSession completableSession;
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

        completableSession = AiSession.builder()
                .userId(1L)
                .difficulty(Difficulty.EASY)
                .attemptNumber(1)
                .isCompleted(false)
                .userIngredientIds("[]")
                .ingredientIdsJson("[1]")
                .build();

        lastAiMessage = AiMessage.builder()
                .session(completableSession)
                .role(Role.AI)
                .messageType(MessageType.INITIAL_REQUEST)
                .content(VALID_AI_RESPONSE_JSON)
                .build();

        savedAiRecipe = AiRecipe.builder()
                .id(99L)
                .title("테스트 레시피")
                .session(completableSession)
                .userId(1L)
                .ingredientsJson("[]")
                .stepsJson("[]")
                .build();

        // 공통 stub
        given(aiSessionRepository.findByIdAndUserId(anyLong(), eq(1L)))
                .willReturn(Optional.of(completableSession));
        given(aiMessageRepository.findTopBySessionAndRoleOrderByCreatedAtDesc(any(), eq(Role.AI)))
                .willReturn(Optional.of(lastAiMessage));
        given(aiRecipeRepository.save(any(AiRecipe.class))).willReturn(savedAiRecipe);
        given(aiMessageRepository.save(any(AiMessage.class))).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(aiMessageRepository).flush();
        given(dailyRecipeRepository.existsByAiRecipe_Session_Id(anyLong())).willReturn(false);
    }

    // 유통기한 임박(leftDays=0) 재료 생성 헬퍼
    private UserIngredient buildUrgentIngredient(Long id) {
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
    private UserIngredient buildNormalIngredient(Long id) {
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
        @DisplayName("레시피 채택 시 COOKING 목표 진행을 호출한다")
        void 레시피채택_COOKING_목표진행_호출() {
            UserIngredient ingredient = buildNormalIngredient(1L);
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(ingredient));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(false);

            aiRecipeService.adoptRecipe(1L, 10L);

            verify(weeklyGoalService).handleGoalProgress(1L, GoalActionType.COOKING);
        }

        @Test
        @DisplayName("채택 시 COOKING 목표 달성이면 weeklyGoalAchieved=true를 반환한다")
        void 레시피채택_COOKING_목표달성_true반환() {
            UserIngredient ingredient = buildNormalIngredient(1L);
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(ingredient));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(true);

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isTrue();
        }

        @Test
        @DisplayName("채택 시 COOKING 목표 미달성이면 weeklyGoalAchieved=false를 반환한다")
        void 레시피채택_COOKING_목표미달성_false반환() {
            UserIngredient ingredient = buildNormalIngredient(1L);
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), eq(1L)))
                    .willReturn(List.of(ingredient));
            given(weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING)).willReturn(false);

            AiRecipeAdoptResponseDto result = aiRecipeService.adoptRecipe(1L, 10L);

            assertThat(result.isWeeklyGoalAchieved()).isFalse();
        }

        @Test
        @DisplayName("이미 완료된 세션을 채택하면 COOKING 목표 진행을 호출하지 않는다")
        void 이미완료된세션_COOKING_목표_미호출() {
            completableSession.complete(); // isCompleted=true
            // 이미 완료 → AppException 발생 → verify 전에 예외가 던져짐
            // 예외 발생 자체가 목표 미호출의 증거
            try {
                aiRecipeService.adoptRecipe(1L, 10L);
            } catch (Exception ignored) {}

            verify(weeklyGoalService, never()).handleGoalProgress(any(), any());
        }
    }

}
