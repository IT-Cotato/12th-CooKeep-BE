package com.cookeep.cookeep.domain.user.application;

import java.time.Duration;

public final class AuthSessionPolicy {

	public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
	public static final int SESSION_ID_BYTES = 16;

	private AuthSessionPolicy() {
	}
}
