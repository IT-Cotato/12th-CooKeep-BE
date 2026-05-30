package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "쿠키", description = "쿠키 관련 API")
@RestController
@RequestMapping("/api/cookies")
@RequiredArgsConstructor
public class PendingCookieRewardController {

    private final CookieService cookieService;

    @Operation(
            summary = "대기 중인 쿠키 보상 수령",
            description = "서버에서 발생시킨 보상 쿠키를 프론트 요청 시점에 지급합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "보상 수령 성공"),
            @ApiResponse(responseCode = "400", description = "이미 수령한 보상"),
            @ApiResponse(responseCode = "403", description = "본인의 보상이 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 보상")
    })
    @PostMapping("/pending/{pendingRewardId}/claim")
    public ResponseEntity<DataResponse<Integer>> claimPendingReward(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long pendingRewardId) {
        int cookieCnt = cookieService.claimPendingReward(userId, pendingRewardId);
        return ResponseEntity.ok(DataResponse.from(cookieCnt));
    }
}
