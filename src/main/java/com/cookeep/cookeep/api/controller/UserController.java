package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.user.UserProvider;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "유저", description = "사용자 정보 및 쿠키 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CookieService cookieService;
    private final UserProvider userProvider; // 원활한 테스트 위해 만든 것. 추후 로그인 및 회원가입 이후에 협의 후 제거하거나 삭제 예정

    @Operation(summary = "보유 쿠키 조회", description = "현재 내가 보유하고 있는 쿠키의 총 개수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/me/cookies")
    public DataResponse<Integer> getMyCookies() {
        // UserProvider를 통해 현재 유저 ID 추출
        Long userId = userProvider.getCurrentUserId();

        // CookieService에서 실제 유저 엔티티의 cookieCnt 반환
        return DataResponse.from(cookieService.getMyCookies(userId));
    }
}
