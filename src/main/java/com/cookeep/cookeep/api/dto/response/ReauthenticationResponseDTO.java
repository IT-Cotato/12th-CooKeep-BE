package com.cookeep.cookeep.api.dto.response;

public record ReauthenticationResponseDTO(
	String reauthToken,
	long expiresInSeconds
) {
}
