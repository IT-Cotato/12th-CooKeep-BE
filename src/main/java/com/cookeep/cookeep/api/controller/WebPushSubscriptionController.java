package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.WebPushSubscriptionRequestDto;
import com.cookeep.cookeep.api.dto.response.WebPushEligibilityResponseDto;
import com.cookeep.cookeep.api.dto.response.WebPushSubscriptionResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.notification.application.WebPushSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "웹 푸시 알림 구독", description = "Web Push 구독 등록 / 삭제 / 수신 가능 여부 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/web/push")
public class WebPushSubscriptionController {

    private final WebPushSubscriptionService webPushSubscriptionService;

    @Operation(
            summary = "웹 푸시 구독 등록",
            description = "브라우저가 생성한 푸시 알림 구독 정보를 서버에 저장. 동일 endpoint 있으면 유지 & 성공 응답"
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_ENDPOINT,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "구독 등록 성공"),
            @ApiResponse(responseCode = "400", description = """
                    잘못된 요청입니다.
                    - INVALID_REQUEST: 요청 값이 올바르지 않음
                    - INVALID_ENDPOINT: subscription endpoint 정보 누락
                    """, content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PostMapping("/subscription")
    public ResponseEntity<DataResponse<WebPushSubscriptionResponseDto>> subscribe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody WebPushSubscriptionRequestDto request
    ) {
        WebPushSubscriptionResponseDto response = webPushSubscriptionService.subscribe(userId, request);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "웹 푸시 구독 삭제",
            description = "서버에 저장된 Push Subscription을 endpoint 기준으로 삭제합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.FORBIDDEN,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.SUBSCRIPTION_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "구독 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "본인 소유 구독이 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = """
                    리소스를 찾을 수 없습니다.
                    - USER_NOT_FOUND: 사용자 없음
                    - SUBSCRIPTION_NOT_FOUND: 해당 subscription 없음
                    """, content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @DeleteMapping("/subscription")
    public ResponseEntity<DataResponse<WebPushSubscriptionResponseDto>> unsubscribe(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody WebPushSubscriptionRequestDto request
    ) {
        WebPushSubscriptionResponseDto response = webPushSubscriptionService.unsubscribe(userId, request);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Operation(
            summary = "웹 푸시 알림 수신 가능 여부 확인",
            description = "현재 유저가 웹 푸시 알림을 받을 수 있는 상태인지 확인"
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/eligibility")
    public ResponseEntity<DataResponse<WebPushEligibilityResponseDto>> checkEligibility(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        WebPushEligibilityResponseDto response = webPushSubscriptionService.checkEligibility(userId);
        return ResponseEntity.ok(DataResponse.from(response));
    }

}