package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequestDTO(
	@NotBlank(message = "전화번호는 필수 입력 값입니다.")
	@Pattern(
		regexp = "^010\\d{8}$",
		message = "휴대폰 번호를 다시 확인해주세요"
	)
	String phoneNumber,
	@NotBlank(message = "인증번호는 필수 입력 값입니다.")
	@Pattern(
		regexp = "^\\d{6}$",
		message = "인증번호는 6자리 숫자입니다."
	)
	String code
) {
}
