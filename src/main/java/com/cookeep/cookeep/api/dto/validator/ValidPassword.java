package com.cookeep.cookeep.api.dto.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target({
	ElementType.FIELD,
	ElementType.METHOD,
	ElementType.PARAMETER,
	ElementType.ANNOTATION_TYPE,
	ElementType.TYPE_USE,
	ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPasswordValidator.class)
public @interface ValidPassword {

	String message() default "영문, 숫자 포함 8자 이상의 비밀번호를 사용해주세요";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
