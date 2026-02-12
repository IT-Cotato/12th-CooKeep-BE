package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.NicknameUpdateRequestDto;
import com.cookeep.cookeep.api.dto.request.UpdateEmailRequestDTO;
import com.cookeep.cookeep.api.dto.response.UserProfileResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.user.application.UserInfoService;
import com.cookeep.cookeep.security.UserPrincipal;

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

@Tag(name = "회원 정보", description = "회원 정보 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserInfoController {

    private final UserInfoService userInfoService;

    // 회원정보 조회
    @Operation(summary = "회원정보 조회 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "요청 파라미터 오류"),
        @ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @PostMapping("/profile")
    public ResponseEntity<DataResponse<UserProfileResponseDTO>> getMyProfile(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.userId();
        return ResponseEntity.ok(DataResponse.from(userInfoService.getMyProfile(userId)));
    }


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
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestBody @Valid NicknameUpdateRequestDto request
    ) {
        userInfoService.updateNickname(userId, request);

        return ResponseEntity.ok(DataResponse.ok());
    }

    // 이메일 변경
    @Operation(summary = "이메일 변경 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "요청 파라미터 오류(@Valid 검증 실패, 현재 이메일과 동일 등)"),
        @ApiResponse(responseCode = "401", description = "회원 인증 실패, AccessToken이 없거나 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음(소셜 로그인 유저는 이메일 변경 불가 등)"),
        @ApiResponse(responseCode = "409", description = "요청 충돌(이미 사용 중인 이메일 등)")
    })
    @PostMapping("/email")
    public ResponseEntity<DataResponse<Void>> updateMyEmail(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UpdateEmailRequestDTO updateEmailRequestDTO
    ) {
        Long userId = principal.userId();
        userInfoService.updateMyEmail(userId, updateEmailRequestDTO);
        return ResponseEntity.ok(DataResponse.ok());
    }
}
