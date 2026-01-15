package com.cookeep.cookeep.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// DTO에서 요구로 하는 필드만 받아오도록 함
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponseDTO(
	@JsonProperty("id")
	Long id,

	@JsonProperty("kakao_account")
	KakaoAccount kakaoAccount
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record KakaoAccount(
		@JsonProperty("email")
		String email
	) {}
}
