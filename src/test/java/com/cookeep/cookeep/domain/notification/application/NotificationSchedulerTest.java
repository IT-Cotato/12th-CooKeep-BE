package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.domain.ingredient.useringredient.application.UserIngredientScheduler;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationSchedulerTest {

    @InjectMocks
    private UserIngredientScheduler scheduler;

    @Mock
    private UserIngredientRepository userIngredientRepository;

    @Mock
    private WebPushNotificationService webPushNotificationService;

    @Nested
    @DisplayName("sendDailyExpirationPush - 스케줄러 알림 전송")
    class SendDailyExpirationPush {

        @Test
        @DisplayName("대상 유저가 없으면 sendExpirationAlert를 호출하지 않는다")
        void 대상유저없음_전송안함() {
            given(userIngredientRepository
                    .findUserIdsWithExpiringTodayAndMarketingConsent(LocalDate.now()))
                    .willReturn(List.of());

            scheduler.sendDailyExpirationPush();

            verify(webPushNotificationService, never()).sendExpirationAlert(anyLong());
        }

        @Test
        @DisplayName("대상 유저가 3명이면 sendExpirationAlert를 3회 호출한다")
        void 대상유저3명_3회호출() {
            given(userIngredientRepository
                    .findUserIdsWithExpiringTodayAndMarketingConsent(LocalDate.now()))
                    .willReturn(List.of(1L, 2L, 3L));

            scheduler.sendDailyExpirationPush();

            verify(webPushNotificationService, times(1)).sendExpirationAlert(1L);
            verify(webPushNotificationService, times(1)).sendExpirationAlert(2L);
            verify(webPushNotificationService, times(1)).sendExpirationAlert(3L);
        }

        @Test
        @DisplayName("특정 유저 전송 중 예외가 발생해도 나머지 유저는 계속 전송한다")
        void 특정유저_예외발생_나머지_계속전송() {
            given(userIngredientRepository
                    .findUserIdsWithExpiringTodayAndMarketingConsent(LocalDate.now()))
                    .willReturn(List.of(1L, 2L, 3L));

            // 2번 유저에서만 예외 발생
            doThrow(new RuntimeException("전송 실패"))
                    .when(webPushNotificationService).sendExpirationAlert(2L);

            scheduler.sendDailyExpirationPush();

            // 예외 발생해도 1, 3번 유저는 정상 호출
            verify(webPushNotificationService, times(1)).sendExpirationAlert(1L);
            verify(webPushNotificationService, times(1)).sendExpirationAlert(2L);
            verify(webPushNotificationService, times(1)).sendExpirationAlert(3L);
        }

        @Test
        @DisplayName("모든 유저 전송 중 예외가 발생해도 스케줄러가 중단되지 않는다")
        void 모든유저_예외발생_스케줄러_중단안됨() {
            given(userIngredientRepository
                    .findUserIdsWithExpiringTodayAndMarketingConsent(LocalDate.now()))
                    .willReturn(List.of(1L, 2L));

            doThrow(new RuntimeException("전송 실패"))
                    .when(webPushNotificationService).sendExpirationAlert(anyLong());

            // 예외가 외부로 전파되지 않아야 함
            org.assertj.core.api.Assertions.assertThatNoException()
                    .isThrownBy(() -> scheduler.sendDailyExpirationPush());

            verify(webPushNotificationService, times(1)).sendExpirationAlert(1L);
            verify(webPushNotificationService, times(1)).sendExpirationAlert(2L);
        }

        @Test
        @DisplayName("DB 조회 자체에서 예외가 발생하면 sendExpirationAlert는 호출되지 않는다")
        void DB조회_예외발생_전송안함() {
            given(userIngredientRepository
                    .findUserIdsWithExpiringTodayAndMarketingConsent(LocalDate.now()))
                    .willThrow(new RuntimeException("DB 오류"));

            // DB 예외는 스케줄러 밖으로 전파될 수 있음 (의도적 미처리)
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> scheduler.sendDailyExpirationPush()
            ).isInstanceOf(RuntimeException.class);

            verify(webPushNotificationService, never()).sendExpirationAlert(anyLong());
        }
    }

}
