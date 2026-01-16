package com.cookeep.cookeep.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	//400
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", "COMMON-001"),
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 잘못되었습니다.", "COMMON-002"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.", "COMMON-003"),
	INVALID_CATEGORY_TYPE(HttpStatus.NOT_FOUND, "category가 존재하지 않습니다.", "INGREDIENT-002"),
	INVALID_STORAGE_TYPE(HttpStatus.NOT_FOUND, "storage가 존재하지 않습니다.", "INGREDIENT-003"),
	CUSTOM_INGREDIENT_REQUIRED_FIELDS_MISSING(HttpStatus.NOT_FOUND, "name, expirationDays, category, storage는 필수 필드입니다.", "INGREDIENT-004"),

	//401
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다.", "COMMON-005"),

	//409
	DUPLICATE_CUSTOM_INGREDIENT(HttpStatus.CONFLICT, "이미 등록된 커스텀 식재료입니다.", "INGREDIENT-001"),

	//500
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 에러가 발생하였습니다.", "COMMON-004"),
	;

	private final HttpStatus httpStatus;
	private final String message;
	private final String code;
}