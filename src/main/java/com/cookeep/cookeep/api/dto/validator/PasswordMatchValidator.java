package com.cookeep.cookeep.api.dto.validator;

import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator
	implements ConstraintValidator<PasswordMatch, SignupRequestDTO> {
	// PasswordMatch의 구현체, 비밀번호 필드 일치 여부를 검증함

	@Override
	public boolean isValid(SignupRequestDTO dto, ConstraintValidatorContext context) {
		// dto의 password와 passwordConfirm값이 같은지 비교
		return dto.password().equals(dto.passwordConfirm());
	}
}
