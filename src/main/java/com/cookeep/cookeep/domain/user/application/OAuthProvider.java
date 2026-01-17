package com.cookeep.cookeep.domain.user.application;

import com.cookeep.cookeep.domain.user.dto.KakaoUserInfoResponseDTO;
import com.cookeep.cookeep.domain.user.entity.Provider;

public interface OAuthProvider {
	Provider provider();
	String getKakaoAccessToken(String code);
	KakaoUserInfoResponseDTO getKakaoUserInfo(String accessToken);
}
