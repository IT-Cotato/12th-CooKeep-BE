package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.api.dto.request.ConsumeIngredientsRequestDto;
import com.cookeep.cookeep.api.dto.response.ConsumeIngredientsResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.ConsumeIngredientService;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.application.ConsumptionReportService;
import com.cookeep.cookeep.domain.onboarding.application.WeeklyGoalService;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ConsumeIngredientServiceTest {

    @Mock private UserIngredientRepository userIngredientRepository;
    @Mock private CookieService cookieService;
    @Mock private ConsumptionReportService consumptionReportService;
    @Mock private WeeklyGoalService weeklyGoalService;

    @InjectMocks
    private ConsumeIngredientService consumeIngredientService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().nickname("테스터").build();
        // 쿠키 지급은 기본 false
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
                .quantity(1)
                .unit(Unit.PIECE)
                .storage(Storage.FRIDGE)
                .expirationDate(LocalDate.now().plusDays(7)) // +7일
                .user(user)
                .build());
    }

    private ConsumeIngredientsRequestDto buildRequest(List<Long> ids) {
        ConsumeIngredientsRequestDto dto = mock(ConsumeIngredientsRequestDto.class);
        given(dto.getUserIngredientIds()).willReturn(ids);
        return dto;
    }

    @Nested
    @DisplayName("consumeIngredients - USE_EXPIRING_INGREDIENT 목표 연동")
    class UseExpiringIngredientGoal {

        @Test
        @DisplayName("임박 재료 1개 소비 시 목표 진행을 1회 호출한다")
        void 임박재료_1개_목표진행_1회() {
            UserIngredient urgent = buildUrgentIngredient();
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(List.of(urgent));

            consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(1L)));

            verify(weeklyGoalService, times(1))
                    .handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);
        }

        @Test
        @DisplayName("임박 재료 3개 소비 시 목표 진행을 3회 호출한다")
        void 임박재료_3개_목표진행_3회() {
            List<UserIngredient> urgents = List.of(
                    buildUrgentIngredient(),
                    buildUrgentIngredient(),
                    buildUrgentIngredient()
            );
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(urgents);

            consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(1L, 2L, 3L)));

            verify(weeklyGoalService, times(3))
                    .handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);
        }

        @Test
        @DisplayName("일반 재료만 소비 시 USE_EXPIRING_INGREDIENT 목표 진행을 호출하지 않는다")
        void 일반재료만_목표진행_미호출() {
            UserIngredient normal = buildNormalIngredient();
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(List.of(normal));

            consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(1L)));

            verify(weeklyGoalService, never())
                    .handleGoalProgress(anyLong(), eq(GoalActionType.USE_EXPIRING_INGREDIENT));
        }

        @Test
        @DisplayName("임박 재료 1개 + 일반 재료 2개 소비 시 목표 진행을 1회만 호출한다")
        void 임박재료_1개_일반재료_2개_목표진행_1회() {
            List<UserIngredient> mixed = List.of(
                    buildUrgentIngredient(),
                    buildNormalIngredient(),
                    buildNormalIngredient()
            );
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(mixed);

            consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(1L, 2L, 3L)));

            verify(weeklyGoalService, times(1))
                    .handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);
        }

        @Test
        @DisplayName("목표 달성 이후에도 handleGoalProgress는 호출되지만 내부에서 false를 반환한다")
        void 목표달성후_추가소비_호출은하되_false반환() {
            // handleGoalProgress 내부에서 isAchieved=true이면 false 반환 (WeeklyGoalService 로직)
            // ConsumeIngredientService는 반환값과 무관하게 임박 재료 수만큼 호출하도록 함
            given(weeklyGoalService.handleGoalProgress(anyLong(), any())).willReturn(false);

            UserIngredient urgent = buildUrgentIngredient();
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(List.of(urgent));

            consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(1L)));

            verify(weeklyGoalService, times(1))
                    .handleGoalProgress(1L, GoalActionType.USE_EXPIRING_INGREDIENT);
        }
    }

    @Nested
    @DisplayName("consumeIngredients - 기본 쿠키 지급 연동")
    class BasicCookieGrant {

        @Test
        @DisplayName("하루 첫 소비 시 쿠키가 지급된다")
        void 첫소비_쿠키지급() {
            UserIngredient normal = buildNormalIngredient();
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(List.of(normal));
            given(cookieService.grantDailyCookie(1L, CookieLog.CookieLogType.BASIC_DAILY_FIRST_CONSUME))
                    .willReturn(true);

            ConsumeIngredientsResponseDto result =
                    consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(1L)));

            assertThat(result.getReward().getGranted()).isTrue();
            assertThat(result.getReward().getPoints()).isEqualTo(1);
        }

        @Test
        @DisplayName("당일 이미 지급된 경우 쿠키가 지급되지 않는다")
        void 중복소비_쿠키미지급() {
            UserIngredient normal = buildNormalIngredient();
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(List.of(normal));
            given(cookieService.grantDailyCookie(1L, CookieLog.CookieLogType.BASIC_DAILY_FIRST_CONSUME))
                    .willReturn(false);

            ConsumeIngredientsResponseDto result =
                    consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(1L)));

            assertThat(result.getReward().getGranted()).isFalse();
            assertThat(result.getReward().getPoints()).isZero();
        }
    }

    @Nested
    @DisplayName("consumeIngredients - 입력 검증")
    class InputValidation {

        @Test
        @DisplayName("빈 재료 목록으로 요청하면 예외가 발생한다")
        void 빈목록_예외발생() {
            assertThatThrownBy(() ->
                    consumeIngredientService.consumeIngredients(1L, buildRequest(List.of()))
            ).isInstanceOf(AppException.class);

            verifyNoInteractions(weeklyGoalService);
        }

        @Test
        @DisplayName("존재하지 않는 재료 ID로 요청하면 예외가 발생한다")
        void 없는재료_예외발생() {
            given(userIngredientRepository.findAllByIngredientIdInAndUser_UserId(anyList(), anyLong()))
                    .willReturn(List.of());

            assertThatThrownBy(() ->
                    consumeIngredientService.consumeIngredients(1L, buildRequest(List.of(999L)))
            ).isInstanceOf(AppException.class);

            verifyNoInteractions(weeklyGoalService);
        }
    }

}
