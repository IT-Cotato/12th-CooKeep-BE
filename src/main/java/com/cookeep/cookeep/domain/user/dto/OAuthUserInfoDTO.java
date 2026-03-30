package com.cookeep.cookeep.domain.user.dto;

// 카카오, 구글 모두 공통적으로 소셜ID, 이메일 정보 필요
public record OAuthUserInfoDTO(
	String id,
	String email
) {
}
