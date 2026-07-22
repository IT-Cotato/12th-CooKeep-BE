package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.api.dto.validator.PasswordMatch;
import com.cookeep.cookeep.api.dto.validator.ValidPassword;

import jakarta.validation.constraints.NotBlank;

@PasswordMatch
public record UpdatePasswordRequestDTO(
	// 영문, 숫자 포함 8자 이상 값
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	@ValidPassword
	String password,

	@NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
	String passwordConfirm
) {
}
