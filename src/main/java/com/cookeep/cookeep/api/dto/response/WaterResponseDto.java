package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WaterResponseDto {

    private boolean isFreeWatering;   // 무료 물주기 여부 (첫 물주기 시 쿠키 차감 없음)
    private boolean isJustHarvested;  // 이번 물주기로 수확 완료 여부
    private Long pendingRewardId;     // 수확 완료 시에만 non-null, 프론트가 claim 호출에 사용
    private int cookieCnt;            // 물주기 처리 후 최종 쿠키 개수
}
