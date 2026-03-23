package com.cookeep.cookeep.domain.cookeeps.application;

import com.cookeep.cookeep.api.dto.response.CookeepsOnboardingResponseDto;
import com.cookeep.cookeep.api.dto.response.RankingResponseDto;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private CookeepsService cookeepsService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        given(wateringLogRepository.findTopWateringUsers(any(), any(), any())).willReturn(List.of());
        given(wateringLogRepository.countByUserAndMonth(any(), any(), any())).willReturn(0L);
        given(dailyRecipeRepository.findTopRankedRecipes(any(), any(), any())).willReturn(List.of());
    }

    @Nested
    @DisplayName("getRanking - 물주기 랭킹 월별 기준")
    class WateringRanking {

        @Test
        @DisplayName("물주기 랭킹 조회 시 이번 달 1일 00:00:00부터 다음 달 1일 00:00:00 범위로 조회한다")
        void 물주기_랭킹_이번달_범위로_조회() {
            cookeepsService.getRanking(USER_ID);

            ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(wateringLogRepository).findTopWateringUsers(
                    startCaptor.capture(), endCaptor.capture(), any(Pageable.class));

            LocalDateTime expectedStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime expectedEnd = expectedStart.plusMonths(1);

            assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
            assertThat(endCaptor.getValue()).isEqualTo(expectedEnd);
        }

        @Test
        @DisplayName("물주기 랭킹은 상위 3명을 1~3위 순위와 함께 반환한다")
        void 물주기_랭킹_상위3명_순위반환() {
            User user1 = User.builder().nickname("유저1").build();
            User user2 = User.builder().nickname("유저2").build();
            User user3 = User.builder().nickname("유저3").build();

            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{user1, 10L});
            rows.add(new Object[]{user2, 7L});
            rows.add(new Object[]{user3, 5L});
            given(wateringLogRepository.findTopWateringUsers(any(), any(), any())).willReturn(rows);

            RankingResponseDto result = cookeepsService.getRanking(USER_ID);

            List<RankingResponseDto.WateringRankDto> ranking = result.getWateringRanking();
            assertThat(ranking).hasSize(3);
            assertThat(ranking.get(0).getRank()).isEqualTo(1);
            assertThat(ranking.get(0).getNickname()).isEqualTo("유저1");
            assertThat(ranking.get(0).getWateringCount()).isEqualTo(10L);
            assertThat(ranking.get(1).getRank()).isEqualTo(2);
            assertThat(ranking.get(1).getNickname()).isEqualTo("유저2");
            assertThat(ranking.get(2).getRank()).isEqualTo(3);
            assertThat(ranking.get(2).getNickname()).isEqualTo("유저3");
        }

        @Test
        @DisplayName("이번 달 물주기 기록이 없으면 빈 리스트를 반환한다")
        void 물주기_없는경우_빈리스트_반환() {
            RankingResponseDto result = cookeepsService.getRanking(USER_ID);

            assertThat(result.getWateringRanking()).isEmpty();
        }

        @Test
        @DisplayName("프로필 식물이 없는 유저는 profileImageUrl이 null로 반환된다")
        void 프로필식물_없는유저_profileImageUrl_null() {
            User user = User.builder().nickname("유저").build();

            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{user, 5L});
            given(wateringLogRepository.findTopWateringUsers(any(), any(), any())).willReturn(rows);

            RankingResponseDto result = cookeepsService.getRanking(USER_ID);

            assertThat(result.getWateringRanking().get(0).getProfileImageUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("getRanking - 나의 이번달 물주기 횟수")
    class MyWateringCount {

        @Test
        @DisplayName("나의 이번달 물주기 횟수를 반환한다")
        void 나의_이번달_물주기_횟수_반환() {
            given(wateringLogRepository.countByUserAndMonth(any(), any(), any())).willReturn(12L);

            RankingResponseDto result = cookeepsService.getRanking(USER_ID);

            assertThat(result.getMyWateringCount()).isEqualTo(12L);
        }

        @Test
        @DisplayName("나의 물주기 횟수 조회 시 이번 달 1일 00:00:00부터 다음 달 1일 00:00:00 범위와 userId로 조회한다")
        void 나의_물주기_횟수_이번달_범위_및_userId로_조회() {
            cookeepsService.getRanking(USER_ID);

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
            RankingResponseDto result = cookeepsService.getRanking(USER_ID);

            assertThat(result.getMyWateringCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("getRanking - 레시피 랭킹 주별 기준 유지")
    class RecipeRanking {

        @Test
        @DisplayName("레시피 랭킹 조회 시 이번 주 월요일 00:00:00부터 정확히 7일 범위로 조회한다")
        void 레시피_랭킹_이번주_7일_범위로_조회() {
            cookeepsService.getRanking(USER_ID);

            ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(dailyRecipeRepository).findTopRankedRecipes(
                    startCaptor.capture(), endCaptor.capture(), any(Pageable.class));

            assertThat(endCaptor.getValue()).isEqualTo(startCaptor.getValue().plusDays(7));
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
}
