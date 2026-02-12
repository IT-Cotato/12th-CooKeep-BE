package com.cookeep.cookeep.api.dto.validator;


import java.lang.reflect.Method;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

	@Override
	public boolean isValid(Object dto, ConstraintValidatorContext context) {
		if (dto == null) return true;

		try {
			// 해당 객체 안에 password와 passwordConfirm이 있는지 확인
			Method password = dto.getClass().getMethod("password");
			Method passwordConfirm = dto.getClass().getMethod("passwordConfirm");

			Object pw = password.invoke(dto);
			Object cpw = passwordConfirm.invoke(dto);

			// null은 @NotBlank 어노테이션에서 잡고 있으므로 실패 처리 하지 않고 역할을 분리함
			if (pw == null || cpw == null) return true;
			return pw.equals(cpw);
		} catch (Exception e) {
			return false;
		}
	}
}