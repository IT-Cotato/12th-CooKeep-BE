package com.cookeep.cookeep.domain.user.application;

import java.time.Duration;

public final class ReauthenticationPolicy {

	public static final Duration TOKEN_TTL = Duration.ofMinutes(5);
	public static final long EXPIRES_IN_SECONDS = TOKEN_TTL.toSeconds();

	private ReauthenticationPolicy() {
	}
}
