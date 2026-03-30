package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.api.dto.response.AiRecipeAdoptResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
    @Mock private AiRateLimitService rateLimitService;

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
        // 기본값: Rate Limit 통과
        willDoNothing().given(rateLimitService).validate(anyLong());
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

    @Nested
    @DisplayName("Rate Limit - AiRateLimitService 연동")
    class RateLimitIntegration {

        // regenerateRecipe 테스트에 필요한 세션 세팅 헬퍼
        private void stubRegenerateSession() throws Exception {
            // userIngredientIds에 실제 JSON을 넣어야 readIngredientsFromSession이 동작
            String ingredientsJson = """
                    [{"ingredientId":1,"name":"양파","quantity":null,"unit":"개"}]
                    """;
            session = AiSession.builder()
                    .userId(1L)
                    .difficulty(Difficulty.EASY)
                    .attemptNumber(1)
                    .isCompleted(false)
                    .userIngredientIds(ingredientsJson)
                    .ingredientIdsJson("[1]")
                    .build();

            given(aiSessionRepository.findByIdAndUserId(anyLong(), eq(1L)))
                    .willReturn(Optional.of(session));
            given(aiSessionRepository.save(any(AiSession.class))).willAnswer(inv -> inv.getArgument(0));
            given(aiMessageRepository.findAllBySessionIdAndRoleAi(anyLong())).willReturn(List.of());
        }

        private void stubGeminiAndYoutube() {
            // GeminiRecipeResponseDto 스텁
            var ingredientsDto = new com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto();
            // ObjectMapper로 직접 만들기 어려우므로 geminiService 자체를 스텁
            com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto response =
                    buildValidGeminiResponse();

            given(geminiService.generateRecipeWithExclusion(anyList(), any(), anyList()))
                    .willReturn(response);
            given(youtubeSearchService.searchVideos(anyList())).willReturn(List.of());
            given(aiMessageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(aiSessionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        // 공통 헬퍼

        /**
         * 리플렉션 없이 GeminiRecipeResponseDto를 ObjectMapper로 생성.
         * VALID_AI_RESPONSE_JSON을 재활용합니다.
         */
        private com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto buildValidGeminiResponse() {
            try {
                return objectMapper.readValue(
                        VALID_AI_RESPONSE_JSON,
                        com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto.class
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 검증: rateLimitService.validate()가 호출되는지 확인

        @Test
        @DisplayName("regenerateRecipe 호출 시 rateLimitService.validate(userId)가 1회 호출된다")
        void regenerate_호출시_validate_1회() throws Exception {
            stubRegenerateSession();
            stubGeminiAndYoutube();

            aiRecipeService.regenerateRecipe(1L, 10L);

            verify(rateLimitService, times(1)).validate(1L);
        }

        @Test
        @DisplayName("regenerateRecipe에서 Rate Limit 초과 시 AI가 호출되지 않는다")
        void regenerate_RateLimit_초과시_Gemini_미호출() throws Exception {
            stubRegenerateSession();

            // validate가 예외를 던지도록 설정
            doThrow(new AppException(ErrorCode.USER_RATE_LIMIT_EXCEEDED))
                    .when(rateLimitService).validate(1L);

            assertThatThrownBy(() -> aiRecipeService.regenerateRecipe(1L, 10L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_RATE_LIMIT_EXCEEDED);

            // Rate Limit에 걸렸으므로 Gemini는 절대 호출되면 안 됨
            verify(geminiService, never()).generateRecipeWithExclusion(anyList(), any(), anyList());
        }

        @Test
        @DisplayName("regenerateRecipe Rate Limit 초과 시 AI_RATE_LIMIT_EXCEEDED 에러코드를 반환한다")
        void regenerate_RateLimit_에러코드_확인() throws Exception {
            stubRegenerateSession();

            doThrow(new AppException(ErrorCode.USER_RATE_LIMIT_EXCEEDED))
                    .when(rateLimitService).validate(1L);

            assertThatThrownBy(() -> aiRecipeService.regenerateRecipe(1L, 10L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.USER_RATE_LIMIT_EXCEEDED);
                    });
        }

        @Test
        @DisplayName("이미 완료된 세션이면 Rate Limit 검증 전에 예외가 발생한다")
        void 완료된세션_RateLimit_미호출() throws Exception {
            stubRegenerateSession();
            session.complete(); // isCompleted = true

            // 완료된 세션이므로 SESSION_ALREADY_COMPLETED 예외 발생
            assertThatThrownBy(() -> aiRecipeService.regenerateRecipe(1L, 10L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_ALREADY_COMPLETED);

            // Rate Limit 검증까지 도달하지 않으므로 validate 미호출
            verify(rateLimitService, never()).validate(anyLong());
        }

        @Test
        @DisplayName("재시도 횟수 초과 시 Rate Limit 검증 전에 예외가 발생한다")
        void 재시도횟수_초과_RateLimit_미호출() throws Exception {
            stubRegenerateSession();

            // attemptNumber를 MAX_RETRY_COUNT(5) 이상으로 설정
            session = AiSession.builder()
                    .userId(1L)
                    .difficulty(Difficulty.EASY)
                    .attemptNumber(5) // MAX_RETRY_COUNT = 5
                    .isCompleted(false)
                    .userIngredientIds("[{\"ingredientId\":1,\"name\":\"양파\",\"quantity\":null,\"unit\":\"개\"}]")
                    .ingredientIdsJson("[1]")
                    .build();
            given(aiSessionRepository.findByIdAndUserId(anyLong(), eq(1L)))
                    .willReturn(Optional.of(session));

            assertThatThrownBy(() -> aiRecipeService.regenerateRecipe(1L, 10L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_RECIPE_CHANGE_LIMIT_EXCEEDED);

            verify(rateLimitService, never()).validate(anyLong());
        }

        @Test
        @DisplayName("서로 다른 유저는 Rate Limit이 독립적으로 적용된다")
        void 서로다른_유저_RateLimit_독립() throws Exception {
            stubRegenerateSession();
            stubGeminiAndYoutube();

            // 유저2 세션 추가 세팅
            AiSession session2 = AiSession.builder()
                    .userId(2L)
                    .difficulty(Difficulty.EASY)
                    .attemptNumber(1)
                    .isCompleted(false)
                    .userIngredientIds("[{\"ingredientId\":1,\"name\":\"양파\",\"quantity\":null,\"unit\":\"개\"}]")
                    .ingredientIdsJson("[1]")
                    .build();
            given(aiSessionRepository.findByIdAndUserId(anyLong(), eq(2L)))
                    .willReturn(Optional.of(session2));

            // 유저1: 통과
            // 유저2: 통과
            aiRecipeService.regenerateRecipe(1L, 10L);
            aiRecipeService.regenerateRecipe(2L, 10L);

            // 각 유저에 대해 독립적으로 validate 1회씩 호출됨
            verify(rateLimitService, times(1)).validate(1L);
            verify(rateLimitService, times(1)).validate(2L);
        }
    }

}
