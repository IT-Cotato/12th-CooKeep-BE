package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.api.dto.request.WebPushSubscriptionRequestDto;
import com.cookeep.cookeep.api.dto.response.WebPushEligibilityResponseDto;
import com.cookeep.cookeep.api.dto.response.WebPushSendResponseDto;
import com.cookeep.cookeep.api.dto.response.WebPushSubscriptionResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.notification.dao.WebPushSubscriptionRepository;
import com.cookeep.cookeep.domain.notification.entity.WebPushSubscription;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import nl.martijndwars.webpush.PushService;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WebPushSubscriptionServiceTest {

    private String validP256dh;
    private String validAuth;

    @BeforeAll
    static void registerBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @InjectMocks
    private WebPushSubscriptionService webPushSubscriptionService;

    @InjectMocks
    private WebPushNotificationService webPushNotificationService;

    @Mock
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Mock
    private UserReader userReader;

    @Mock
    private UserIngredientRepository userIngredientRepository;

    @Mock
    private PushService pushService;

    private static final Long USER_ID = 1L;
    private static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/test-endpoint-12345";

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = mock(User.class);

        validP256dh = generateValidP256dh();
        validAuth = generateValidAuth();

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
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(ENDPOINT, validP256dh, validAuth));

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
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(ENDPOINT, validP256dh, validAuth));

            assertThat(result.getMessage()).isEqualTo("웹 푸시 구독이 등록되었습니다.");
            verify(webPushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 유저이면 USER_NOT_FOUND 예외가 발생한다")
        void 존재하지않는_유저_예외발생() {
            given(userReader.readById(USER_ID))
                    .willThrow(new AppException(ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() ->
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(ENDPOINT, validP256dh, validAuth))
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
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest(null, validP256dh, validAuth))
            )
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.INVALID_ENDPOINT.getMessage());

            verify(webPushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("endpoint가 빈 문자열이면 INVALID_ENDPOINT 예외가 발생한다")
        void endpoint_공백_INVALID_ENDPOINT_예외발생() {
            assertThatThrownBy(() ->
                    webPushSubscriptionService.subscribe(USER_ID, buildRequest("  ", validP256dh, validAuth))
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
                    webPushSubscriptionService.unsubscribe(USER_ID, buildRequest(ENDPOINT, validP256dh, validAuth));

            assertThat(result.getMessage()).isEqualTo("웹 푸시 구독이 해제되었습니다.");
            verify(webPushSubscriptionRepository, times(1)).delete(subscription);
        }

        @Test
        @DisplayName("존재하지 않는 endpoint이면 SUBSCRIPTION_NOT_FOUND 예외가 발생한다")
        void 존재하지않는_endpoint_SUBSCRIPTION_NOT_FOUND_예외발생() {
            given(webPushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    webPushSubscriptionService.unsubscribe(USER_ID, buildRequest(ENDPOINT, validP256dh, validAuth))
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
                    webPushSubscriptionService.unsubscribe(USER_ID, buildRequest(ENDPOINT, validP256dh, validAuth))
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

    @Nested
    @DisplayName("4. sendExpirationAlert - 전송 조건 검증")
    class SendExpirationAlert {

        @Test
        @DisplayName("마케팅 수신 미동의면 전송하지 않고 sent=false와 수신 미동의 메시지를 반환한다")
        void 수신미동의_전송안함_sent_false() throws Exception {
            given(user.getMarketingConsent()).willReturn(false);

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            assertThat(result.getSent()).isFalse();
            assertThat(result.getMessage()).isEqualTo("알림 수신 동의가 되어있지 않습니다.");
            verify(userIngredientRepository, never()).existsByUserIdAndExpirationDate(anyLong(), any());
            verify(webPushSubscriptionRepository, never()).findAllByUser_UserId(anyLong());
            verify(pushService, never()).send(any());
        }

        @Test
        @DisplayName("marketingConsent가 null이면 미동의로 처리해 sent=false를 반환한다")
        void marketingConsent_null_sent_false() throws Exception {
            given(user.getMarketingConsent()).willReturn(null);

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            assertThat(result.getSent()).isFalse();
            assertThat(result.getMessage()).isEqualTo("알림 수신 동의가 되어있지 않습니다.");
            verify(pushService, never()).send(any());
        }

        @Test
        @DisplayName("당일 만료 재료가 없으면 전송하지 않고 sent=false와 재료 없음 메시지를 반환한다")
        void 만료재료없음_전송안함_sent_false() throws Exception {
            // given
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(false);

            // when
            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            // then
            assertThat(result.getSent()).isFalse();
            assertThat(result.getMessage()).isEqualTo("유통기한이 만료된 재료가 없습니다.");
            verify(webPushSubscriptionRepository, never()).findAllByUser_UserId(anyLong());
            verify(pushService, never()).send(any());
        }

        @Test
        @DisplayName("구독 정보가 없으면 sent=false와 구독없음 메시지를 반환한다")
        void 구독정보없음_sent_false() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of());

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            assertThat(result.getSent()).isFalse();
            assertThat(result.getMessage()).isEqualTo("구독 정보가 없습니다.");
            verify(pushService, never()).send(any());
        }

        @Test
        @DisplayName("존재하지 않는 유저이면 USER_NOT_FOUND 예외가 발생한다")
        void 존재하지않는_유저_예외발생() {
            given(userReader.readById(USER_ID))
                    .willThrow(new AppException(ErrorCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> webPushNotificationService.sendExpirationAlert(USER_ID))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

            verifyNoInteractions(userIngredientRepository, webPushSubscriptionRepository, pushService);
        }

    }

    @Nested
    @DisplayName("4. sendExpirationAlert - 전송 성공/실패 결과")
    class SendResult {

        @Test
        @DisplayName("201 응답이면 sent=true와 성공 메시지를 반환한다")
        void 응답201_sent_true() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(buildSubscription(10L, ENDPOINT, validP256dh, validAuth)));
            CloseableHttpResponse response = mockResponse(201);
            given(pushService.send(any())).willReturn(response);

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            assertThat(result.getSent()).isTrue();
            assertThat(result.getMessage()).isEqualTo("유통기한임박 알림이 전송되었습니다.");
        }

        @Test
        @DisplayName("모든 구독이 410 응답이면 구독 삭제 후 sent=false와 만료 메시지를 반환한다")
        void 모든구독_410응답_sent_false_만료메시지() throws Exception {

            // 410 → 구독 삭제 → successCount=0 → sent=false 반환
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription subscription = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(subscription));
            CloseableHttpResponse response = mockResponse(410);
            given(pushService.send(any())).willReturn(response);

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            // 만료 구독 삭제
            verify(webPushSubscriptionRepository, times(1)).delete(subscription);
            // 실제 전달 없음 → sent=false
            assertThat(result.getSent()).isFalse();
            assertThat(result.getMessage()).isEqualTo("유효한 구독이 없어 알림을 전송하지 못했습니다.");
        }

        @Test
        @DisplayName("모든 구독이 404 응답이면 구독 삭제 후 sent=false와 만료 메시지를 반환한다")
        void 모든구독_404응답_sent_false_만료메시지() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription subscription = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(subscription));
            CloseableHttpResponse response = mockResponse(404);
            given(pushService.send(any())).willReturn(response);

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            verify(webPushSubscriptionRepository, times(1)).delete(subscription);
            assertThat(result.getSent()).isFalse();
            assertThat(result.getMessage()).isEqualTo("유효한 구독이 없어 알림을 전송하지 못했습니다.");
        }

        @Test
        @DisplayName("구독 2개 중 1개가 410이면 해당 구독만 삭제하고 나머지 성공 → sent=true를 반환한다")
        void 구독2개_1개410_1개201_sent_true() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription sub1 = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            WebPushSubscription sub2 = buildSubscription(11L, ENDPOINT + "-2", validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(sub1, sub2));

            // sub1 → 410(만료), sub2 → 201(성공)
            CloseableHttpResponse response410 = mockResponse(410);
            CloseableHttpResponse response201 = mockResponse(201);

            given(pushService.send(any()))
                    .willReturn(response410)
                    .willReturn(response201);

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            verify(webPushSubscriptionRepository, times(1)).delete(sub1);
            verify(webPushSubscriptionRepository, never()).delete(sub2);
            assertThat(result.getSent()).isTrue();
        }

        @Test
        @DisplayName("구독 2개가 모두 410이면 모두 삭제하고 sent=false를 반환한다")
        void 구독2개_모두410_sent_false() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription sub1 = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            WebPushSubscription sub2 = buildSubscription(11L, ENDPOINT + "-2", validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(sub1, sub2));
            CloseableHttpResponse response410 = mockResponse(410);

            given(pushService.send(any()))
                    .willReturn(response410)
                    .willReturn(response410);

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            verify(webPushSubscriptionRepository, times(1)).delete(sub1);
            verify(webPushSubscriptionRepository, times(1)).delete(sub2);
            assertThat(result.getSent()).isFalse();
        }

        @Test
        @DisplayName("모든 구독에서 예외가 발생하면 sent=false와 만료 메시지를 반환한다")
        void 모든구독_예외발생_sent_false() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription subscription = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(subscription));
            given(pushService.send(any())).willThrow(new RuntimeException("네트워크 오류"));

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            // 예외가 발생한 구독은 삭제하지 않음
            verify(webPushSubscriptionRepository, never()).delete(any());
            // 성공 없음 → sent=false
            assertThat(result.getSent()).isFalse();
            assertThat(result.getMessage()).isEqualTo("유효한 구독이 없어 알림을 전송하지 못했습니다.");
        }

        @Test
        @DisplayName("구독 2개 중 1개 예외, 1개 성공이면 예외 구독은 삭제하지 않고 sent=true를 반환한다")
        void 구독2개_1개예외_1개성공_sent_true() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription sub1 = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            WebPushSubscription sub2 = buildSubscription(11L, ENDPOINT + "-2", validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(sub1, sub2));

            // sub1 → 예외, sub2 → 201
            given(pushService.send(any()))
                    .willThrow(new RuntimeException("push send failed"))
                    .willReturn(mockResponse(201));

            WebPushSendResponseDto result = webPushNotificationService.sendExpirationAlert(USER_ID);

            verify(webPushSubscriptionRepository, never()).delete(any());
            assertThat(result.getSent()).isTrue();
            // 전송 자체는 2회 시도
            verify(pushService, times(2)).send(any());
        }
    }

    @Nested
    @DisplayName("4. sendExpirationAlert - 페이로드 내용 검증")
    class PayloadVerification {

        @Test
        @DisplayName("전송 payload에는 title, body, url, type 정보가 포함된다")
        void payload_내용검증() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription subscription = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(subscription));
            CloseableHttpResponse response = mockResponse(201);
            given(pushService.send(any())).willReturn(response);

            ArgumentCaptor<nl.martijndwars.webpush.Notification> captor =
                    ArgumentCaptor.forClass(nl.martijndwars.webpush.Notification.class);

            webPushNotificationService.sendExpirationAlert(USER_ID);

            verify(pushService).send(captor.capture());
            String payload = new String(captor.getValue().getPayload(), StandardCharsets.UTF_8);

            assertThat(payload).contains("title");
            assertThat(payload).contains("body");
            assertThat(payload).contains("url");
            assertThat(payload).contains("EXPIRATION");
        }

        @Test
        @DisplayName("EXPIRATION 타입의 title은 '유통기한임박'이다")
        void EXPIRATION_title_검증() throws Exception {
            given(user.getMarketingConsent()).willReturn(true);
            given(userIngredientRepository.existsByUserIdAndExpirationDate(eq(USER_ID), any()))
                    .willReturn(true);

            WebPushSubscription subscription = buildSubscription(10L, ENDPOINT, validP256dh, validAuth);
            given(webPushSubscriptionRepository.findAllByUser_UserId(USER_ID))
                    .willReturn(List.of(subscription));
            CloseableHttpResponse response = mockResponse(201);
            given(pushService.send(any())).willReturn(response);

            ArgumentCaptor<nl.martijndwars.webpush.Notification> captor =
                    ArgumentCaptor.forClass(nl.martijndwars.webpush.Notification.class);

            webPushNotificationService.sendExpirationAlert(USER_ID);

            verify(pushService).send(captor.capture());
            String payload = new String(captor.getValue().getPayload(), StandardCharsets.UTF_8);

            assertThat(payload).contains("유통기한임박");
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
                .p256dh(validP256dh)
                .auth(validAuth)
                .build();
        ReflectionTestUtils.setField(sub, "id", id);
        return sub;
    }

    private WebPushSubscription buildSubscription(Long id, String endpoint, String p256dh, String auth) {
        WebPushSubscription subscription = WebPushSubscription.builder()
                .endpoint(endpoint)
                .p256dh(p256dh)
                .auth(auth)
                .build();

        ReflectionTestUtils.setField(subscription, "id", id);
        return subscription;
    }

    private CloseableHttpResponse mockResponse(int statusCode) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        given(response.getStatusLine()).willReturn(statusLine);
        given(statusLine.getStatusCode()).willReturn(statusCode);
        return response;
    }

    private String generateValidP256dh() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        byte[] x = publicKey.getW().getAffineX().toByteArray();
        byte[] y = publicKey.getW().getAffineY().toByteArray();

        byte[] uncompressed = new byte[65];
        uncompressed[0] = 0x04;
        System.arraycopy(toUnsignedFixedLength(x, 32), 0, uncompressed, 1, 32);
        System.arraycopy(toUnsignedFixedLength(y, 32), 0, uncompressed, 33, 32);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressed);
    }

    private byte[] toUnsignedFixedLength(byte[] value, int length) {
        byte[] result = new byte[length];

        if (value.length == length) {
            return value;
        }

        if (value.length == length + 1 && value[0] == 0) {
            System.arraycopy(value, 1, result, 0, length);
            return result;
        }

        if (value.length < length) {
            System.arraycopy(value, 0, result, length - value.length, value.length);
            return result;
        }

        System.arraycopy(value, value.length - length, result, 0, length);
        return result;
    }

    private String generateValidAuth() {
        byte[] authBytes = new byte[16];
        for (int i = 0; i < authBytes.length; i++) {
            authBytes[i] = (byte) (i + 1);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(authBytes);
    }

}
