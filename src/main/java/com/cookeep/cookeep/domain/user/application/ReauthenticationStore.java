package com.cookeep.cookeep.domain.user.application;

import com.cookeep.cookeep.domain.user.entity.ReauthenticationPurpose;

public interface ReauthenticationStore {

	String issue(Long userId, ReauthenticationPurpose purpose);

	void consume(Long userId, ReauthenticationPurpose purpose, String rawToken);
}
