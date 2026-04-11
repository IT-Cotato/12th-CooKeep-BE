package com.cookeep.cookeep.domain.ingredient.application;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListCreateResponseDto;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.RecentIngredientService;
import com.cookeep.cookeep.domain.ingredient.useringredient.application.UserIngredientServiceImpl;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.application.ConsumptionReportService;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserIngredientServiceRewardTest {

    @Mock private UserIngredientRepository userIngredientRepository;
    @Mock private DefaultIngredientRepository defaultIngredientRepository;
    @Mock private ConsumptionReportService consumptionReportService;
    @Mock private UserRepository userRepository;
    @Mock private RecentIngredientService recentIngredientService;
    @Mock private CookieService cookieService;

    @InjectMocks
    private UserIngredientServiceImpl userIngredientService;

    private User firstTimeUser;   // ONBOARDING_INGREDIENT 미지급 상태
    private User rewardedUser;    // ONBOARDING_INGREDIENT 이미 지급 상태
    private DefaultIngredient defaultIngredient;

    @BeforeEach
    void setUp() {
        firstTimeUser = User.builder()
                .nickname("신규유저")
                .isFirstIngredientReward(false)
                .build();

        rewardedUser = User.builder()
                .nickname("기존유저")
                .isFirstIngredientReward(true)
                .build();

        defaultIngredient = mock(DefaultIngredient.class);
        given(defaultIngredient.getId()).willReturn(1L);
        given(defaultIngredient.getIngredient()).willReturn("양파");
        given(defaultIngredient.getImageUrl()).willReturn("https://example.com/onion.png");
        given(defaultIngredient.getUnit()).willReturn(Unit.PIECE);
        given(defaultIngredient.getDefaultStorage()).willReturn(Storage.FRIDGE);
        given(defaultIngredient.getDefaultExpirationDays()).willReturn(7);

        given(defaultIngredientRepository.findById(1L)).willReturn(Optional.of(defaultIngredient));
        given(userIngredientRepository.save(any(UserIngredient.class)))
                .willAnswer(inv -> inv.getArgument(0));
        doNothing().when(consumptionReportService).createLogForNewIngredient(any(), any());
        doNothing().when(recentIngredientService).saveBatch(anyLong(), any());
    }

    private UserIngredientCreateRequestDto buildRequest() {
        UserIngredientCreateRequestDto req = new UserIngredientCreateRequestDto();
        ReflectionTestUtils.setField(req, "type", Type.DEFAULT);
        ReflectionTestUtils.setField(req, "referenceId", 1L);
        return req;
    }

    @Nested
    @DisplayName("createAll - ONBOARDING_INGREDIENT 쿠키 지급 검증")
    class OnboardingIngredientReward {

        @Test
        @DisplayName("최초 식재료 등록 시 ingredientRewardGranted=true를 반환한다")
        void 최초등록_ingredientRewardGranted_true() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(firstTimeUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.isIngredientRewardGranted()).isTrue();
        }

        @Test
        @DisplayName("최초 식재료 등록 시 RewardInfo.granted=true를 반환한다")
        void 최초등록_RewardInfo_granted_true() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(firstTimeUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.getReward()).isNotNull();
            assertThat(result.getReward().getGranted()).isTrue();
        }

        @Test
        @DisplayName("최초 식재료 등록 시 RewardInfo.grantedTypes에 ONBOARDING_INGREDIENT가 포함된다")
        void 최초등록_RewardInfo_grantedTypes_포함() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(firstTimeUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.getReward().getGrantedTypes())
                    .containsExactly(CookieLog.CookieLogType.ONBOARDING_INGREDIENT);
        }

        @Test
        @DisplayName("최초 식재료 등록 시 RewardInfo.points=1을 반환한다")
        void 최초등록_RewardInfo_points_1() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(firstTimeUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.getReward().getPoints()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 보상을 받은 유저는 ingredientRewardGranted=false를 반환한다")
        void 기존유저_ingredientRewardGranted_false() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(rewardedUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.isIngredientRewardGranted()).isFalse();
        }

        @Test
        @DisplayName("이미 보상을 받은 유저는 RewardInfo.granted=false를 반환한다")
        void 기존유저_RewardInfo_granted_false() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(rewardedUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.getReward().getGranted()).isFalse();
        }

        @Test
        @DisplayName("이미 보상을 받은 유저는 RewardInfo.grantedTypes가 비어있다")
        void 기존유저_RewardInfo_grantedTypes_empty() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(rewardedUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.getReward().getGrantedTypes()).isEmpty();
        }

        @Test
        @DisplayName("이미 보상을 받은 유저는 RewardInfo.points=0을 반환한다")
        void 기존유저_RewardInfo_points_0() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(rewardedUser));

            UserIngredientListCreateResponseDto result =
                    userIngredientService.createAll(1L, List.of(buildRequest()));

            assertThat(result.getReward().getPoints()).isZero();
        }

        @Test
        @DisplayName("최초 등록 시 cookieService.updateCookie가 ONBOARDING_INGREDIENT 타입으로 호출된다")
        void 최초등록_cookieService_호출() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(firstTimeUser));

            userIngredientService.createAll(1L, List.of(buildRequest()));

            verify(cookieService).updateCookie(1L, CookieLog.CookieLogType.ONBOARDING_INGREDIENT);
        }

        @Test
        @DisplayName("이미 보상을 받은 유저는 cookieService.updateCookie가 호출되지 않는다")
        void 기존유저_cookieService_미호출() {
            given(userRepository.findById(anyLong())).willReturn(Optional.of(rewardedUser));

            userIngredientService.createAll(1L, List.of(buildRequest()));

            verify(cookieService, never()).updateCookie(anyLong(), any());
        }
    }
}
