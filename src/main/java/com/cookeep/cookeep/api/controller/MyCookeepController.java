package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.MyProfileResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.config.UserPrincipal;
import com.cookeep.cookeep.domain.user.application.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이쿠킵", description = "마이쿠킵 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my-cookeep")
public class MyCookeepController {

    private final UserInfoService userInfoService;

    @Operation(summary = "마이쿠킵 프로필 조회", description = "닉네임, 프로필 식물 이미지, 가입 일수, 이번 주 목표를 조회합니다.")
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공",
                    content = @Content(schema = @Schema(implementation = MyProfileResponseDto.class))),
            @ApiResponse(responseCode = "401", description = """
                인증 실패입니다. 다음 오류가 발생할 수 있습니다:
                - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                - USER_NOT_FOUND: 존재하지 않는 사용자입니다.
                """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                서버 오류입니다. 다음 오류가 발생할 수 있습니다:
                - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                """, content = @Content)
    })
    @GetMapping("/profile")
    public ResponseEntity<DataResponse<MyProfileResponseDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.userId();
        MyProfileResponseDto response = userInfoService.getProfile(userId);

        return ResponseEntity.ok(DataResponse.from(response));
    }
}
