package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.api.dto.validator.PasswordMatch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@PasswordMatch
public record ResetPasswordRequestDTO(
	@NotBlank(message = "전화번호는 필수 입력 값입니다.")
	@Pattern(
		regexp = "^010\\d{8}$",
		message = "휴대폰 번호를 다시 확인해주세요"
	)
	String phoneNumber,

	// 영문, 숫자 포함 8자 이상 값
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
		message = "영문, 숫자 포함 8자 이상의 비밀번호를 사용해주세요"
	)
	String password,

	@NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
	String passwordConfirm
) {
}
