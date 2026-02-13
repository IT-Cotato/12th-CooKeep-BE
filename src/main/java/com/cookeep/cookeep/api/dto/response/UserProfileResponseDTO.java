package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.user.entity.Provider;

public record UserProfileResponseDTO(
	String Nickname,
	String phoneNumber,
	String email,
	Provider authProvider,
	Boolean marketingPush
) {
}
