package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.NicknameUpdateRequestDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.common.util.AuthUtils;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.security.JwtTokenProvider;
import com.cookeep.cookeep.domain.user.application.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 정보", description = "회원 정보 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserInfoController {

    private final UserInfoService userInfoService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 수정합니다.")
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.DUPLICATE_NICKNAME,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 수정 성공"),
            @ApiResponse(responseCode = "401", description = """
                인증 실패입니다. 다음 오류가 발생할 수 있습니다:
                - UNAUTHORIZED: 인증 정보가 없거나 유효하지 않습니다.
                """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
                리소스를 찾을 수 없습니다. 다음 오류가 발생할 수 있습니다:
                - USER_NOT_FOUND: 존재하지 않는 사용자입니다.
                """, content = @Content),
            @ApiResponse(responseCode = "409", description = """
                충돌이 발생했습니다. 다음 오류가 발생할 수 있습니다:
                - DUPLICATE_NICKNAME: 이미 사용 중인 닉네임입니다.
                """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
                서버 오류입니다. 다음 오류가 발생할 수 있습니다:
                - INTERNAL_SERVER_ERROR: 서버 내부 오류가 발생했습니다.
                """, content = @Content)
    })
    @PatchMapping("/nickname")
    public ResponseEntity<DataResponse<Void>> updateNickname(
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid NicknameUpdateRequestDto request
    ) {
        String token = AuthUtils.extractBearerToken(authorization);
        Long userId = jwtTokenProvider.getUserId(token, false);

        userInfoService.updateNickname(userId, request);

        return ResponseEntity.ok(DataResponse.ok());
    }
}
