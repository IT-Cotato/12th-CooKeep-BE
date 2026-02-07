package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "쿠키", description = "쿠키 관련 API")
@RestController
@RequestMapping("/api/users/me/cookies")
@RequiredArgsConstructor
public class CookieController {

    private final CookieService cookieService;

    @Operation(summary = "보유 쿠키 조회", description = "현재 내가 보유하고 있는 쿠키의 총 개수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않음")
    })
    @GetMapping
    public DataResponse<Integer> getMyCookies(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        return DataResponse.from(cookieService.getMyCookies(userId));
    }
}
