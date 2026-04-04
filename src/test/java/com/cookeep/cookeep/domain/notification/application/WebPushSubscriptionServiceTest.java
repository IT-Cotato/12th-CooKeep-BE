package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.api.dto.request.WebPushSubscriptionRequestDto;
import com.cookeep.cookeep.api.dto.response.WebPushEligibilityResponseDto;
import com.cookeep.cookeep.api.dto.response.WebPushSubscriptionResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebPushSubscriptionServiceTest {

    @InjectMocks
    private WebPushSubscriptionService webPushSubscriptionService;

    @Mock
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Mock
    private UserReader userReader;

    private static final Long USER_ID = 1L;
    private static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/test-endpoint-12345";
    private static final String P256DH   = "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQt";
    private static final String AUTH     = "tBHItJI5svbpez7KI4CCXg==";

    private User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(USER_ID);
        lenient().when(userReader.readById(USER_ID)).thenReturn(user);
    }

    // 1. 구독 등록
    @Nested
    @DisplayName("1. subscribe - 구독 등록")
    class Subscribe {

        @Test
        @DisplayName("신규 endpoint이면 저장 후 성공 메시지를 반환한다")
        void 신규_endpoint_저장_성공메시지_반환() {
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT)).willReturn(Optional.empty());

            WebPushSubscriptionResponseDto result =
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(ENDPOINT, P256DH, AUTH));

            assertThat(result.getMessage()).isEqualTo("웹 푸시 구독이 등록되었습니다.");
            verify(webPushSubscriptionRepository, times(1)).save(any(WebPushSubscription.class));
        }

        @Test
        @DisplayName("이미 동일한 endpoint가 존재하면 저장하지 않고 성공 메시지를 반환한다")
        void 중복_endpoint_저장_생략_성공메시지_반환() {
            WebPushSubscription existing = buildSubscription(10L, user, ENDPOINT);
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .willReturn(Optional.of(existing));

            WebPushSubscriptionResponseDto result =
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(ENDPOINT, P256DH, AUTH));

            assertThat(result.getMessage()).isEqualTo("웹 푸시 구독이 등록되었습니다.");
            verify(webPushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 유저이면 USER_NOT_FOUND 예외가 발생한다")
        void 존재하지않는_유저_예외발생() {
            given(userReader.readById(USER_ID))
                    .willThrow(new AppException(ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() ->
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(ENDPOINT, P256DH, AUTH))
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

            verify(webPushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("request가 null이면 INVALID_REQUEST 예외가 발생한다")
        void request_null_INVALID_REQUEST_예외발생() {
            assertThatThrownBy(() ->
                    webPushSubscriptionService.subscribe(USER_ID, null)
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.INVALID_REQUEST.getMessage());

            verify(webPushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("endpoint가 null이면 INVALID_ENDPOINT 예외가 발생한다")
        void endpoint_null_INVALID_ENDPOINT_예외발생() {
            assertThatThrownBy(() ->
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(null, P256DH, AUTH))
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.INVALID_ENDPOINT.getMessage());

            verify(webPushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("endpoint가 빈 문자열이면 INVALID_ENDPOINT 예외가 발생한다")
        void endpoint_공백_INVALID_ENDPOINT_예외발생() {
            assertThatThrownBy(() ->
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest("  ", P256DH, AUTH))
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.INVALID_ENDPOINT.getMessage());

            verify(webPushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("keys가 null이면 INVALID_REQUEST 예외가 발생한다")
        void keys_null_INVALID_REQUEST_예외발생() {
            WebPushSubscriptionRequestDto request = new WebPushSubscriptionRequestDto();
            ReflectionTestUtils.setField(request, "endpoint", ENDPOINT);
            // keys 필드를 명시적으로 null로 유지

            assertThatThrownBy(() ->
                    webPushSubscriptionService.subscribe(USER_ID, request)
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.INVALID_REQUEST.getMessage());

            verify(webPushSubscriptionRepository, never()).save(any());
        }
    }

    // 2. 구독 삭제
    @Nested
    @DisplayName("2. unsubscribe - 구독 삭제")
    class Unsubscribe {

        @Test
        @DisplayName("본인 소유 endpoint이면 삭제 후 성공 메시지를 반환한다")
        void 본인_소유_endpoint_삭제_성공메시지_반환() {
            WebPushSubscription subscription = buildSubscription(10L, user, ENDPOINT);
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .willReturn(Optional.of(subscription));

            WebPushSubscriptionResponseDto result =
                    webPushSubscriptionService.unsubscribe(USER_ID, buildRequest(ENDPOINT, P256DH, AUTH));

            assertThat(result.getMessage()).isEqualTo("웹 푸시 구독이 해제되었습니다.");
            verify(webPushSubscriptionRepository, times(1)).delete(subscription);
        }

        @Test
        @DisplayName("존재하지 않는 endpoint이면 SUBSCRIPTION_NOT_FOUND 예외가 발생한다")
        void 존재하지않는_endpoint_SUBSCRIPTION_NOT_FOUND_예외발생() {
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    webPushSubscriptionService.unsubscribe(USER_ID, buildRequest(ENDPOINT, P256DH, AUTH))
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.SUBSCRIPTION_NOT_FOUND.getMessage());

            verify(webPushSubscriptionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("다른 유저 소유의 endpoint를 삭제하려 하면 FORBIDDEN 예외가 발생한다")
        void 타인_소유_endpoint_FORBIDDEN_예외발생() {
            User anotherUser = mock(User.class);
            given(anotherUser.getUserId()).willReturn(999L);

            WebPushSubscription subscription = buildSubscription(10L, anotherUser, ENDPOINT);
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .willReturn(Optional.of(subscription));

            assertThatThrownBy(() ->
                    webPushSubscriptionService.unsubscribe(USER_ID, buildRequest(ENDPOINT, P256DH, AUTH))
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.FORBIDDEN.getMessage());

            verify(webPushSubscriptionRepository, never()).delete(any());
        }
    }

    // 3. 구독 가능 여부 확인
    @Nested
    @DisplayName("3. checkEligibility - 수신 가능 여부 확인")
    class CheckEligibility {

        @Test
        @DisplayName("구독 정보가 존재하면 eligible=true를 반환한다")
        void 구독_존재_eligible_true_반환() {
            given(webPushSubscriptionRepository.existsByUser_UserId(USER_ID)).willReturn(true);

            WebPushEligibilityResponseDto result =
                    webPushSubscriptionService.checkEligibility(USER_ID);

            assertThat(result.getEligible()).isTrue();
        }

        @Test
        @DisplayName("구독 정보가 없으면 eligible=false를 반환한다")
        void 구독_없음_eligible_false_반환() {
            given(webPushSubscriptionRepository.existsByUser_UserId(USER_ID)).willReturn(false);

            WebPushEligibilityResponseDto result =
                    webPushSubscriptionService.checkEligibility(USER_ID);

            assertThat(result.getEligible()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 유저이면 USER_NOT_FOUND 예외가 발생한다")
        void 존재하지않는_유저_USER_NOT_FOUND_예외발생() {
            given(userReader.readById(USER_ID))
                    .willThrow(new AppException(ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() ->
                    webPushSubscriptionService.checkEligibility(USER_ID)
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

            verify(webPushSubscriptionRepository, never()).existsByUser_UserId(any());
        }
    }

    // --- 내부 메서드 ---

    // Keys 객체 생성
    private WebPushSubscriptionRequestDto buildRequest(String endpoint, String p256dh, String auth) {
        WebPushSubscriptionRequestDto dto = new WebPushSubscriptionRequestDto();
        ReflectionTestUtils.setField(dto, "endpoint", endpoint);

        if (p256dh != null || auth != null) {
            WebPushSubscriptionRequestDto.Keys keys = new WebPushSubscriptionRequestDto.Keys();
            ReflectionTestUtils.setField(keys, "p256dh", p256dh);
            ReflectionTestUtils.setField(keys, "auth", auth);
            ReflectionTestUtils.setField(dto, "keys", keys);
        }
        return dto;
    }

    // 테스트용 WebPushSubscription 생성
    private WebPushSubscription buildSubscription(Long id, User owner, String endpoint) {
        WebPushSubscription sub = WebPushSubscription.builder()
                .user(owner)
                .endpoint(endpoint)
                .p256dh(P256DH)
                .auth(AUTH)
                .build();
        ReflectionTestUtils.setField(sub, "id", id);
        return sub;
    }

}
