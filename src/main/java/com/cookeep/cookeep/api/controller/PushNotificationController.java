package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.WebPushSendResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.notification.application.PopupNotificationEligibilityService;
import com.cookeep.cookeep.domain.notification.application.WebPushNotificationService;
import com.cookeep.cookeep.domain.notification.dto.PushNotificationEligibilityResponseDto;
import com.cookeep.cookeep.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "(MAIN04) 푸시 알림", description = "유통기한 임박 식재료 팝업 알림 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class PushNotificationController {

    private final PopupNotificationEligibilityService popupNotificationEligibilityService;
    private final WebPushNotificationService webPushNotificationService;

    @Operation(
            summary = "1. 유통기한 임박 팝업 자격 확인 (모달 팝업 알림)",
            description = """
                    유통기한이 당일(D-0)인 식재료가 존재하는지 확인하여 팝업을 띄울지 결정합니다.
                    
                    - 당일 만료 식재료가 있으면 eligible: true 반환. 프론트가 호출하여 푸시 알림 전송
                    - 당일 만료 식재료가 없으면 eligible: false만 반환
                    
                    프론트엔드는 앱 진입 시 이 API를 호출하여 팝업 표시 여부를 결정합니다.
                    """
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = """
                    인증 실패입니다.
                    - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                    서버 오류입니다.
                    - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                    """, content = @Content)
    })
    @GetMapping("/alerts")
    public ResponseEntity<DataResponse<PushNotificationEligibilityResponseDto>> checkPushNotificationEligibility(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        PushNotificationEligibilityResponseDto response =
                popupNotificationEligibilityService.checkEligibility(userId);

        return ResponseEntity.ok(DataResponse.from(response));
    }

//    @Operation(
//            summary = "2. 유통기한 임박 웹 팝업 알림 전송",
//            description = """
//                    유통기한이 당일(D-0)인 식재료가 있는 경우 웹 푸시 알림을 전송합니다.
//
//                    **전송 조건 (모두 충족 시 전송)**
//                    1. 마케팅 수신 동의(marketingConsent) = true
//                    2. 냉장고 속 leftDays = 0 인 식재료 존재
//                    3. 서버에 구독 정보(WebPushSubscription) 존재
//
//                    - 조건 미충족 시 sent = false 반환 (에러 아님)
//                    - 만료된 구독(410/404 응답)은 자동 삭제
//                    - 알림 타입: EXPIRATION (유통기한 임박)
//                    """
//    )
//    @ApiErrorCodeExamples({
//            ErrorCode.UNAUTHORIZED,
//            ErrorCode.USER_NOT_FOUND,
//            ErrorCode.SUBSCRIPTION_NOT_FOUND,
//            ErrorCode.INTERNAL_SERVER_ERROR
//    })
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "알림 전송 성공 또는 전송 조건 미충족 (sent 필드로 구분)"),
//            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
//            @ApiResponse(responseCode = "404", description = """
//                    리소스를 찾을 수 없습니다.
//                    - USER_NOT_FOUND: 사용자 없음
//                    - SUBSCRIPTION_NOT_FOUND: 구독 없음
//                    """, content = @Content),
//            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
//    })
//    @PostMapping("/web/push/notifications")
//    public ResponseEntity<DataResponse<WebPushSendResponseDto>> sendAlert(
//            @AuthenticationPrincipal(expression = "userId") Long userId
//    ) {
//        WebPushSendResponseDto response = webPushNotificationService.sendExpirationAlert(userId);
//        return ResponseEntity.ok(DataResponse.from(response));
//    }

    // ===== 아래는 FCM 실제 연동 테스트용 엔드포인트입니다. =====

    @Operation(summary = "[테스트] 식물 시듦 알림 전송", description = "로그인한 유저에게 PLANT_WILTING 웹 푸시를 즉시 전송합니다. (FCM 연동 테스트용)")
    @PostMapping("/web/push/test/plant-wilting")
    public ResponseEntity<DataResponse<WebPushSendResponseDto>> testPlantWiltingPush(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        WebPushSendResponseDto response =
                webPushNotificationService.sendPlantStatusAlert(userId, NotificationType.PLANT_WILTING);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(summary = "[테스트] 식물 성장정지 알림 전송", description = "로그인한 유저에게 PLANT_GROWTH_STOP 웹 푸시를 즉시 전송합니다. (FCM 연동 테스트용)")
    @PostMapping("/web/push/test/plant-frozen")
    public ResponseEntity<DataResponse<WebPushSendResponseDto>> testPlantFrozenPush(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        WebPushSendResponseDto response =
                webPushNotificationService.sendPlantStatusAlert(userId, NotificationType.PLANT_GROWTH_STOP);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(summary = "[테스트] 유통기한 임박 알림 전송", description = "로그인한 유저에게 EXPIRATION 웹 푸시를 즉시 전송합니다. (FCM 연동 테스트용)")
    @PostMapping("/web/push/test/expiration")
    public ResponseEntity<DataResponse<WebPushSendResponseDto>> testExpirationPush(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        WebPushSendResponseDto response = webPushNotificationService.sendExpirationAlert(userId);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
