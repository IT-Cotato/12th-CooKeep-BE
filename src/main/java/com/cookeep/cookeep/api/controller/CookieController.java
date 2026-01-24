package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.util.AuthUtils;
import com.cookeep.cookeep.config.JwtTokenProvider;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "쿠키", description = "쿠키 관련 API")
@RestController
@RequestMapping("/api/users/me/cookies")
@RequiredArgsConstructor
public class CookieController {

    private final CookieService cookieService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "보유 쿠키 조회", description = "현재 내가 보유하고 있는 쿠키의 총 개수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않음")
    })
    @GetMapping
    public DataResponse<Integer> getMyCookies(
            @RequestHeader("Authorization") String authorization
    ) {
        // TODO: 추후 시큐리티 설정(JwtAuthenticationFilter)이 완료되면
        // TODO: @AuthenticationPrincipal 또는 커스텀 @LoginUser를 사용하여
        // TODO: 컨트롤러 레이어에서 토큰 파싱 로직을 제거하고 보안 책임을 위임할 예정입니다.

        // AuthUtils를 사용해 안전하게 토큰 추출, null, 잘못된 형식, 공백 등을 자동으로 처리
        String token = AuthUtils.extractBearerToken(authorization);

        // JWT 토큰에서 userId 추출
        Long userId = jwtTokenProvider.getUserId(token, false);

        // CookieService에서 실제 유저 엔티티의 cookieCnt 반환
        return DataResponse.from(cookieService.getMyCookies(userId));
    }
}
