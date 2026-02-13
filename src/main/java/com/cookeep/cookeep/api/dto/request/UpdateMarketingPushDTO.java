package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMarketingPushDTO(
	@NotNull(message = "푸쉬알림 동의 여부는 필수 입력 값입니다.")
	Boolean marketingPush
) {
}
