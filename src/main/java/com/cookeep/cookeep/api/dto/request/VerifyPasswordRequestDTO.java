package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordRequestDTO(
	// 현재 비밀번호
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	String password
) {
}
