package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequestDTO(
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 주소를 다시 확인해주세요")
	String email,
	@NotBlank(message = "인증번호는 필수 입력 값입니다.")
	@Pattern(
		regexp = "^\\d{6}$",
		message = "인증번호는 6자리 숫자입니다."
	)
	String code
) {
}
