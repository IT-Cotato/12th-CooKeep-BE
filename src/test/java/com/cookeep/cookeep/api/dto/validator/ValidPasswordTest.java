package com.cookeep.cookeep.api.dto.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cookeep.cookeep.api.dto.request.ResetPasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.request.UpdatePasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyPasswordRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;

class ValidPasswordTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidatorFactory() {
		validatorFactory.close();
	}

	@ParameterizedTest
	@ValueSource(strings = {"password1", "password1!", "pass word1", "password1??"})
	@DisplayName("new password requests accept passwords with optional special characters")
	void newPasswordRequests_validPassword_passValidation(String password) {
		newPasswordRequests(password, password)
			.forEach(request -> assertThat(validator.validate(request)).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"12345678", "password", "pass1"})
	@DisplayName("new password requests reject passwords that do not meet the shared policy")
	void newPasswordRequests_invalidPassword_failValidation(String password) {
		newPasswordRequests(password, password).forEach(request -> {
			Set<ConstraintViolation<Object>> violations = validator.validate(request);

			assertThat(violations).anySatisfy(violation -> {
				assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
				assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
					.isEqualTo(ValidPassword.class);
			});
		});
	}

	@Test
	@DisplayName("blank new passwords are rejected by NotBlank")
	void newPasswordRequests_blankPassword_failNotBlankValidation() {
		newPasswordRequests("", "").forEach(request -> {
			Set<ConstraintViolation<Object>> violations = validator.validate(request);

			assertThat(violations).anySatisfy(violation -> {
				assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
				assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
					.isEqualTo(NotBlank.class);
			});
			assertThat(violations).noneSatisfy(violation ->
				assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
					.isEqualTo(ValidPassword.class));
		});
	}

	@Test
	@DisplayName("password confirmation must still match")
	void newPasswordRequests_mismatchedConfirmation_failPasswordMatchValidation() {
		newPasswordRequests("password1!", "different1!").forEach(request ->
			assertThat(validator.validate(request)).anySatisfy(violation ->
				assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
					.isEqualTo(PasswordMatch.class)));
	}

	@Test
	@DisplayName("current password verification accepts any non-blank password format")
	void verifyPassword_nonBlankLegacyFormat_passValidation() {
		assertThat(validator.validate(new VerifyPasswordRequestDTO("legacy!"))).isEmpty();
	}
	@Test
	@DisplayName("current password verification rejects a blank password")
	void verifyPassword_blankPassword_failValidation() {
		assertThat(validator.validate(new VerifyPasswordRequestDTO(" "))).anySatisfy(violation ->
			assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.isEqualTo(NotBlank.class));
	}

	private List<Object> newPasswordRequests(String password, String passwordConfirm) {
		return List.of(
			new SignupRequestDTO("user@example.com", password, passwordConfirm, false),
			new ResetPasswordRequestDTO("user@example.com", password, passwordConfirm),
			new UpdatePasswordRequestDTO(password, passwordConfirm)
		);
	}
}
