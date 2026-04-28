package com.cookeep.cookeep.domain.verification.entity;

public enum VerificationPurpose {
	SIGNUP, // 회원가입
	RESET_PASSWORD, // 비밀번호 찾기 - 비밀번호 리셋
	CHANGE_EMAIL, // 이메일 변경
	PASSWORD_VERIFICATION // 비밀번호 변경 - 비밀번호 5회 오류 시 본인 인증
}
