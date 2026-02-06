package com.cookeep.cookeep.api.dto.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {
	// 비밀번호 일치 여부를 검증하는 커스텀 어노테이션
	// 실제 검증 로직은 PasswordMatchValidator에서 수행됨

	String message() default "비밀번호가 일치하지 않습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
