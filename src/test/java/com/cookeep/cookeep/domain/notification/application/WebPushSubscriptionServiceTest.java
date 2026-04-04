package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.api.dto.request.WebPushSubscriptionRequestDto;
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

    private User mockUser;
    private WebPushSubscriptionRequestDto validRequest;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        given(mockUser.getUserId()).willReturn(USER_ID);

        validRequest = makeRequest(ENDPOINT, P256DH, AUTH);
    }

    // 1. 구독 등록
    @Nested
    @DisplayName("1. subscribe (구독 등록)")
    class Subscribe {

        @Test
        @DisplayName("성공 - 신규 endpoint면 저장 후 성공 메시지 반환")
        void subscribe_success_newEndpoint() {
            // given
            given(userReader.readById(USER_ID)).willReturn(mockUser);
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT)).willReturn(Optional.empty());

            // WebPushSubscription 저장 시 id 세팅 모킹
            doAnswer(invocation -> {
                WebPushSubscription sub = invocation.getArgument(0);
                ReflectionTestUtils.setField(sub, "id", 10L);
                return sub;
            }).when(webPushSubscriptionRepository).save(any(WebPushSubscription.class));

            // when
            WebPushSubscriptionResponseDto response = webPushSubscriptionService.subscribe(USER_ID, validRequest);

            // then
            assertThat(response.getMessage()).isEqualTo("웹 푸시 구독이 등록되었습니다.");
            then(webPushSubscriptionRepository).should(times(1)).save(any(WebPushSubscription.class));
        }

        @Test
        @DisplayName("성공 (멱등) - 이미 동일한 endpoint가 존재하면 저장하지 않고 성공 메시지 반환")
        void subscribe_success_duplicateEndpoint_idempotent() {
            // given
            WebPushSubscription existingSubscription = buildSubscription(10L, mockUser, ENDPOINT);
            given(userReader.readById(USER_ID)).willReturn(mockUser);
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .willReturn(Optional.of(existingSubscription));

            // when
            WebPushSubscriptionResponseDto response = webPushSubscriptionService.subscribe(USER_ID, validRequest);

            // then
            assertThat(response.getMessage()).isEqualTo("웹 푸시 구독이 등록되었습니다.");
            then(webPushSubscriptionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 유저")
        void subscribe_fail_userNotFound() {
            // given
            given(userReader.readById(USER_ID))
                    .willThrow(new AppException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> webPushSubscriptionService.subscribe(USER_ID, validRequest))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            then(webPushSubscriptionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("실패 - request 자체가 null")
        void subscribe_fail_requestNull() {
            // given
            given(userReader.readById(USER_ID)).willReturn(mockUser);

            // when & then
            assertThatThrownBy(() -> webPushSubscriptionService.subscribe(USER_ID, null))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("실패 - endpoint가 null")
        void subscribe_fail_endpointNull() {
            // given
            given(userReader.readById(USER_ID)).willReturn(mockUser);
            WebPushSubscriptionRequestDto requestWithNullEndpoint = makeRequest(null, P256DH, AUTH);

            // when & then
            assertThatThrownBy(() -> webPushSubscriptionService.subscribe(USER_ID, requestWithNullEndpoint))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ENDPOINT);
        }

        @Test
        @DisplayName("실패 - endpoint가 빈 문자열")
        void subscribe_fail_endpointBlank() {
            // given
            given(userReader.readById(USER_ID)).willReturn(mockUser);
            WebPushSubscriptionRequestDto requestWithBlankEndpoint = makeRequest("  ", P256DH, AUTH);

            // when & then
            assertThatThrownBy(() -> webPushSubscriptionService.subscribe(USER_ID, requestWithBlankEndpoint))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ENDPOINT);
        }

        @Test
        @DisplayName("실패 - keys가 null")
        void subscribe_fail_keysNull() {
            // given
            given(userReader.readById(USER_ID)).willReturn(mockUser);
            WebPushSubscriptionRequestDto requestWithNullKeys = makeRequest(ENDPOINT, null, null);

            // when & then
            assertThatThrownBy(() -> webPushSubscriptionService.subscribe(USER_ID, requestWithNullKeys))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    // 2. 구독 삭제


    // --- 내부 메서드 ---

    // Keys 객체 생성
    private WebPushSubscriptionRequestDto makeRequest(String endpoint, String p256dh, String auth) {
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
    private WebPushSubscription buildSubscription(Long id, User user, String endpoint) {
        WebPushSubscription sub = WebPushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh(P256DH)
                .auth(AUTH)
                .build();
        ReflectionTestUtils.setField(sub, "id", id);
        return sub;
    }

}
