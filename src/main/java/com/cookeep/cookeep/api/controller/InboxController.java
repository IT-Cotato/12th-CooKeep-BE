package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.GetNoticeInboxResponseDto;
import com.cookeep.cookeep.api.dto.response.GetNotificationResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.notice.application.NoticeService;
import com.cookeep.cookeep.domain.notification.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "알림함", description = "알림 인벤토리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/inbox")
public class InboxController {

    private final NoticeService noticeService;
    private final NotificationService notificationService;

    // 알림함 > 공지사항 목록 조회
    @Operation(
            summary = "알림함 공지사항 목록 조회",
            description = "알림함(공지사항 탭)에 노출할 공지사항을 최근 30일 이내, 최신순으로 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (AccessToken이 없거나 유효하지 않음)", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/notices")
    public ResponseEntity<DataResponse<GetNoticeInboxResponseDto>> getNoticeInbox() {
        return ResponseEntity.ok(DataResponse.from(noticeService.getNoticeInbox()));
    }

    @Operation(
            summary = "알림함 서비스 알림 목록 조회",
            description = "로그인한 유저의 서비스 알림(유통기한 임박, 식물 상태 등) 발송 이력을 최근 30일 이내, 최신순으로 조회합니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (AccessToken이 없거나 유효하지 않음)", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자 없음", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @GetMapping("/notifications")
    public ResponseEntity<DataResponse<List<GetNotificationResponseDto>>> getNotifications(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        List<GetNotificationResponseDto> response = notificationService.getNotifications(userId);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    // 알림함 > 서비스 알림 단건 읽음 처리
    @Operation(
            summary = "알림함 서비스 알림 읽음 처리",
            description = "알림함에서 특정 서비스 알림 1건을 읽음 상태로 변경합니다. 본인 소유의 알림만 처리할 수 있습니다."
    )
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.NOTIFICATION_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (AccessToken이 없거나 유효하지 않음)", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인 소유가 아닌 알림", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<DataResponse<Void>> readNotification(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Parameter(description = "알림 ID", required = true)
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userId, notificationId);

        return ResponseEntity.ok(DataResponse.ok());
    }
}
