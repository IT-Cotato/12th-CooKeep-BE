package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.notification.application.PopupNotificationEligibilityService;
import com.cookeep.cookeep.domain.notification.dto.PushNotificationEligibilityResponseDto;
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

    @Operation(
            summary = "유통기한 임박 팝업 자격 확인",
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

}
