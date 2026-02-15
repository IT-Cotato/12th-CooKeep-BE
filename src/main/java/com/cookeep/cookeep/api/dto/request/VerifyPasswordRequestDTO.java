package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPasswordRequestDTO(
	// 현재 비밀번호
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
		message = "영문, 숫자 포함 8자 이상의 비밀번호를 사용해주세요"
	)
	String password
) {
}
