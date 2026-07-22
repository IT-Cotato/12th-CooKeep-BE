package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.api.dto.validator.PasswordMatch;
import com.cookeep.cookeep.api.dto.validator.ValidPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@PasswordMatch // password와 passwordConfirm 일치 여부를 확인하는 커스텀 어노테이션
public record SignupRequestDTO(
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	@Email(message = "이메일 주소를 다시 확인해주세요")
	String email,

	// 영문, 숫자 포함 8자 이상 값, 특수문자는 선택사항
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	@ValidPassword
	String password,

	@NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
	String passwordConfirm,

	boolean marketingConsent
) {
}
