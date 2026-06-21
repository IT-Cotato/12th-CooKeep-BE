package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(name = "CookieReward", description = "쿠키 리워드 결과")
@Getter
@Builder
public class CookieRewardDto {

    private Boolean granted;

    private Integer points;

    private List<CookieLog.CookieLogType> types;

    private Integer currentCookieCount;
}
