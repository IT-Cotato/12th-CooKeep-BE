package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.WebPushSubscriptionRequestDto;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}