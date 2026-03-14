package com.cookeep.cookeep.domain.onboarding.application;

import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.onboarding.dao.WeeklyGoalRepository;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import com.cookeep.cookeep.domain.onboarding.entity.WeeklyGoal;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyGoalServiceTest {

    @Mock
    private WeeklyGoalRepository weeklyGoalRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private CookieService cookieService;

    @InjectMocks
    private WeeklyGoalService weeklyGoalService;

    private User user;
    private LocalDate weekStart;

    @BeforeEach
    void setUp() {
        user = User.builder().nickname("테스터").build();
        weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        given(userReader.readById(any())).willReturn(user);
    }

    @Nested
    @DisplayName("handleGoalProgress - 목표 카운트 증가")
    class HandleGoalProgress {

        @Test
        @DisplayName("이번 주 목표가 없으면 false를 반환한다")
        void 목표없음_false반환() {
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.empty());

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE);

            assertThat(result).isFalse();
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("목표 타입이 액션과 다르면 false를 반환한다")
        void 목표타입불일치_false반환() {
            WeeklyGoal goal = buildGoal(GoalActionType.PHOTO_RECORD, 1, 0); // targetCount=1: 타입만 맞았다면 이 한 번으로 달성됐을 상황
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE);

            assertThat(result).isFalse();
            assertThat(goal.getCurrentCount()).isZero();
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("이미 달성된 목표는 카운트를 증가시키지 않고 false를 반환한다")
        void 이미달성_false반환() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 1, 0);
            goal.incrementCount(); // targetCount=1이므로 isAchieved=true

            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE);

            assertThat(result).isFalse();
            assertThat(goal.getCurrentCount()).isEqualTo(1); // 그대로
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("목표 카운트가 증가하지만 아직 미달성이면 false를 반환한다")
        void 카운트증가_미달성_false반환() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 3, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE);

            assertThat(result).isFalse();
            assertThat(goal.getCurrentCount()).isEqualTo(1);
            assertThat(goal.isAchieved()).isFalse();
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("목표 달성 시 true를 반환하고 쿠키를 지급한다")
        void 목표달성_true반환_쿠키지급() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 3, 2); // 2/3 진행 중
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE);

            assertThat(result).isTrue();
            assertThat(goal.getCurrentCount()).isEqualTo(3);
            assertThat(goal.isAchieved()).isTrue();
            verify(cookieService).updateCookie(1L, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }

        @Test
        @DisplayName("PHOTO_RECORD 목표도 동일하게 동작한다")
        void PHOTO_RECORD_목표달성() {
            WeeklyGoal goal = buildGoal(GoalActionType.PHOTO_RECORD, 2, 1);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.PHOTO_RECORD);

            assertThat(result).isTrue();
            assertThat(goal.isAchieved()).isTrue();
            verify(cookieService).updateCookie(1L, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }
    }

    @Nested
    @DisplayName("handleGoalUndo - 목표 카운트 감소")
    class HandleGoalUndo {

        @Test
        @DisplayName("미달성 상태에서 취소하면 카운트가 감소한다")
        void 미달성_카운트감소() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 3, 2);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            weeklyGoalService.handleGoalUndo(1L, GoalActionType.RECIPE_LIKE);

            assertThat(goal.getCurrentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("카운트가 0일 때 취소해도 음수가 되지 않는다")
        void 카운트0_취소시_음수방지() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 3, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            weeklyGoalService.handleGoalUndo(1L, GoalActionType.RECIPE_LIKE);

            assertThat(goal.getCurrentCount()).isZero();
        }

        @Test
        @DisplayName("이미 달성된 경우 취소해도 카운트가 변하지 않는다")
        void 달성후_카운트불변() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 1, 0);
            goal.incrementCount(); // isAchieved = true

            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            weeklyGoalService.handleGoalUndo(1L, GoalActionType.RECIPE_LIKE);

            assertThat(goal.getCurrentCount()).isEqualTo(1); // 변화 없음
            assertThat(goal.isAchieved()).isTrue();
        }

        @Test
        @DisplayName("목표가 없으면 아무 동작도 하지 않는다")
        void 목표없음_노옵() {
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.empty());

            weeklyGoalService.handleGoalUndo(1L, GoalActionType.RECIPE_LIKE);

            verifyNoInteractions(cookieService);
        }
    }

    @Nested
    @DisplayName("like/unlike 반복 시나리오")
    class LikeToggleScenario {

        @Test
        @DisplayName("같은 레시피 like-unlike-like 반복 시 카운트는 최대 1이다")
        void 좋아요_토글반복_카운트최대1() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 3, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE); // like → 1
            weeklyGoalService.handleGoalUndo(1L, GoalActionType.RECIPE_LIKE);     // unlike → 0
            weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE); // like → 1
            weeklyGoalService.handleGoalUndo(1L, GoalActionType.RECIPE_LIKE);     // unlike → 0
            weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE); // like → 1

            assertThat(goal.getCurrentCount()).isEqualTo(1);
            assertThat(goal.isAchieved()).isFalse();
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("서로 다른 레시피 3개에 좋아요를 누르면 목표가 달성된다")
        void 다른레시피3개_좋아요_목표달성() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 3, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE); // 레시피A → 1
            weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE); // 레시피B → 2
            boolean achieved = weeklyGoalService.handleGoalProgress(1L, GoalActionType.RECIPE_LIKE); // 레시피C → 3

            assertThat(achieved).isTrue();
            assertThat(goal.getCurrentCount()).isEqualTo(3);
            assertThat(goal.isAchieved()).isTrue();
            verify(cookieService, times(1)).updateCookie(1L, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }
    }

    @Nested
    @DisplayName("handleGoalProgress - COOKING 목표")
    class CookingGoal {

        @Test
        @DisplayName("COOKING 목표 달성 시 true를 반환하고 쿠키를 지급한다")
        void COOKING_목표달성_true반환_쿠키지급() {
            WeeklyGoal goal = buildGoal(GoalActionType.COOKING, 3, 2); // 2/3 진행 중
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING);

            assertThat(result).isTrue();
            assertThat(goal.getCurrentCount()).isEqualTo(3);
            assertThat(goal.isAchieved()).isTrue();
            verify(cookieService).updateCookie(1L, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }

        @Test
        @DisplayName("COOKING 목표 타입 불일치 시 false를 반환한다")
        void COOKING_목표타입불일치_false반환() {
            WeeklyGoal goal = buildGoal(GoalActionType.RECIPE_LIKE, 1, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING);

            assertThat(result).isFalse();
            assertThat(goal.getCurrentCount()).isZero();
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("COOKING 목표 미달성 카운트 증가 시 false를 반환한다")
        void COOKING_카운트증가_미달성_false반환() {
            WeeklyGoal goal = buildGoal(GoalActionType.COOKING, 5, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING);

            assertThat(result).isFalse();
            assertThat(goal.getCurrentCount()).isEqualTo(1);
            assertThat(goal.isAchieved()).isFalse();
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("COOKING 이미 달성된 목표는 추가 카운트 없이 false를 반환한다")
        void COOKING_이미달성_false반환() {
            WeeklyGoal goal = buildGoal(GoalActionType.COOKING, 1, 0);
            goal.incrementCount(); // isAchieved=true

            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.COOKING);

            assertThat(result).isFalse();
            assertThat(goal.getCurrentCount()).isEqualTo(1); // 그대로
            verifyNoInteractions(cookieService);
        }
    }

    @Nested
    @DisplayName("handleGoalProgress - USE_EXPIRING_INGREDIENT 목표")
    class UseExpiringIngredientGoal {

        @Test
        @DisplayName("USE_EXPIRING_INGREDIENT 목표 달성 시 true를 반환하고 쿠키를 지급한다")
        void 임박재료_목표달성_true반환_쿠키지급() {
            WeeklyGoal goal = buildGoal(GoalActionType.USE_EXPIRING_INGREDIENT, 2, 1); // 1/2 진행 중
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);

            assertThat(result).isTrue();
            assertThat(goal.getCurrentCount()).isEqualTo(2);
            assertThat(goal.isAchieved()).isTrue();
            verify(cookieService).updateCookie(1L, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }

        @Test
        @DisplayName("임박 재료 3개를 1개씩 호출하면 쿠키는 목표 달성 시 1회만 지급된다")
        void 임박재료_3회호출_쿠키_1회지급() {
            WeeklyGoal goal = buildGoal(GoalActionType.USE_EXPIRING_INGREDIENT, 3, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT); // 1/3
            weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT); // 2/3
            weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT); // 3/3 달성

            assertThat(goal.isAchieved()).isTrue();
            verify(cookieService, times(1))
                    .updateCookie(1L, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }

        @Test
        @DisplayName("USE_EXPIRING_INGREDIENT 목표 타입 불일치 시 false를 반환한다")
        void 임박재료_타입불일치_false반환() {
            WeeklyGoal goal = buildGoal(GoalActionType.COOKING, 1, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            boolean result = weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);

            assertThat(result).isFalse();
            assertThat(goal.getCurrentCount()).isZero();
            verifyNoInteractions(cookieService);
        }

        @Test
        @DisplayName("달성 이후 추가 호출 시 쿠키가 중복 지급되지 않는다")
        void 달성후_추가호출_쿠키_중복지급없음() {
            WeeklyGoal goal = buildGoal(GoalActionType.USE_EXPIRING_INGREDIENT, 1, 0);
            given(weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart))
                    .willReturn(Optional.of(goal));

            weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT); // 달성
            weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT); // 추가 호출
            weeklyGoalService.handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT); // 추가 호출

            // 쿠키는 최초 달성 시 1회만 지급
            verify(cookieService, times(1))
                    .updateCookie(1L, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
        }
    }

    // 테스트용 WeeklyGoal 빌더 헬퍼
    private WeeklyGoal buildGoal(GoalActionType actionType, int targetCount, int initialCount) {
        WeeklyGoal goal = WeeklyGoal.builder()
                .user(user)
                .goalActionType(actionType)
                .targetCount(targetCount)
                .weekStartDate(weekStart)
                .build();
        for (int i = 0; i < initialCount; i++) {
            goal.incrementCount();
        }
        return goal;
    }
}
