package com.cookeep.cookeep.api.controller;


import com.cookeep.cookeep.api.dto.response.ProfileImageListResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.config.ApiErrorCodeExamples;
import com.cookeep.cookeep.domain.user.application.ProfileImageService;
import com.cookeep.cookeep.domain.user.application.UserInfoService;
import com.cookeep.cookeep.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "프로필 이미지 목록 조회", description = "선택 가능한 프리셋 프로필 이미지 6종을 조회합니다.")
    @ApiErrorCodeExamples({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @GetMapping("/profile-images")
    public ResponseEntity<DataResponse<ProfileImageListResponseDto>> getProfileImages(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.userId();
        return ResponseEntity.ok(DataResponse.from(profileImageService.getProfileImages(userId)));
    }
}
