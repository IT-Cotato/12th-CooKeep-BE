package com.cookeep.cookeep.api.controller;


import com.cookeep.cookeep.api.dto.response.ProfileImageListResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.user.application.ProfileImageService;
import com.cookeep.cookeep.domain.user.application.UserInfoService;
import com.cookeep.cookeep.security.UserPrincipal;
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

@Tag(name = "프로필 이미지", description = "프로필 이미지 목록 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ProfileImageController {

    private final UserInfoService userInfoService;
    private final ProfileImageService profileImageService;

    @Operation(summary = "프로필 이미지 목록 조회", description = "선택 가능한 프로필 이미지 12종을 S3에서 조회합니다.")
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProfileImageListResponseDto.class))),
            @ApiResponse(responseCode = "401", description = """
            인증 실패입니다.
            - UNAUTHORIZED: 인증에 실패했습니다.
            """, content = @Content),
            @ApiResponse(responseCode = "404", description = """
            리소스를 찾을 수 없습니다.
            - USER_NOT_FOUND: 존재하지 않는 사용자입니다.
            """, content = @Content),
            @ApiResponse(responseCode = "500", description = """
            서버 오류입니다.
            - INTERNAL_SERVER_ERROR: 서버 내부에서 에러가 발생하였습니다.
            """, content = @Content)
    })
    @GetMapping("/profile-images")
    public ResponseEntity<DataResponse<ProfileImageListResponseDto>> getProfileImages(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.userId();
        return ResponseEntity.ok(DataResponse.from(profileImageService.getProfileImages(userId)));
    }
}
