package com.cookeep.cookeep.security;

import java.time.Instant;

public record TokenClaims(
	Long userId,
	String sessionId,
	Instant expiresAt
) {
}
