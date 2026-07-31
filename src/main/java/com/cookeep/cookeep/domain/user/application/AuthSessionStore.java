package com.cookeep.cookeep.domain.user.application;

import java.time.Duration;

public interface AuthSessionStore {

	void create(Long userId, String sessionId, String rawRefreshToken, Duration ttl);

	RefreshRotationResult rotate(
		Long userId,
		String sessionId,
		String currentRawToken,
		String nextRawToken,
		Duration remainingTtl
	);

	boolean isActive(Long userId, String sessionId);

	void revoke(Long userId);
}
