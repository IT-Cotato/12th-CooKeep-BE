package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(name = "ClaimPendingRewardResponse", description = "대기 중인 쿠키 보상 수령 응답 DTO")
@Getter
@Builder
public class ClaimPendingRewardResponseDto {

    @Schema(description = "지급된 리워드 정보")
    private CookieRewardDto reward;

    public static ClaimPendingRewardResponseDto from(CookieRewardDto reward) {
        return ClaimPendingRewardResponseDto.builder().reward(reward).build();
    }
}
