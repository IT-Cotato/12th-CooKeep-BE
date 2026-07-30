package com.cookeep.cookeep.domain.user.application;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cookeep.cookeep.api.dto.response.ReauthenticationResponseDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.entity.ReauthenticationPurpose;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReauthenticationService {

	private final ReauthenticationStore reauthenticationStore;

	public ReauthenticationResponseDTO issueForPasswordChange(Long userId) {
		String rawToken = reauthenticationStore.issue(
			userId,
			ReauthenticationPurpose.CHANGE_PASSWORD
		);
		return new ReauthenticationResponseDTO(rawToken, ReauthenticationPolicy.EXPIRES_IN_SECONDS);
	}

	public void assertTokenPresent(String rawToken) {
		if (!StringUtils.hasText(rawToken)) {
			throw new AppException(ErrorCode.REAUTH_TOKEN_REQUIRED);
		}
	}

	public void consumeForPasswordChange(Long userId, String rawToken) {
		assertTokenPresent(rawToken);
		reauthenticationStore.consume(
			userId,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			rawToken
		);
	}
}
