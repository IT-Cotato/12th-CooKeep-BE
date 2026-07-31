package com.cookeep.cookeep.domain.user.application;

import java.time.Instant;

public interface AuthSessionStore {

	void create(Long userId, String sessionId, String rawRefreshToken, Instant expiresAt);

	RefreshRotationResult rotate(
		Long userId,
		String sessionId,
		String currentRawToken,
		String nextRawToken,
		Instant expiresAt
	);

	boolean isActive(Long userId, String sessionId);

	void revoke(Long userId);
}
