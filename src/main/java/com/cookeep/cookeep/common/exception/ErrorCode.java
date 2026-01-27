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
	PLANT_NOT_FROZEN(HttpStatus.BAD_REQUEST, "성장 정지 상태가 아닙니다.", "PLANT-003"),
	ALREADY_HARVESTED(HttpStatus.BAD_REQUEST, "이미 수확한 식물입니다.", "PLANT-004"),
	PLANT_IS_FROZEN(HttpStatus.BAD_REQUEST, "성장 정지 상태의 식물입니다.", "PLANT-005"),
	INGREDIENT_REQUIRED_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "(냉장고에 재료 추가)필수값이 누락되었습니다.", "INGREDIENT-001"),
	INVALID_UNIT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 단위 타입입니다.", "INGREDIENT-002"),
	INVALID_STORAGE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 보관 장소 타입입니다.", "INGREDIENT-003"),
	INVALID_CATEGORY_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리 타입입니다.", "INGREDIENT-004"),
	CUSTOM_INGREDIENT_REQUIRED_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "(커스텀 재료 등록)필수값이 누락되었습니다.", "INGREDIENT-005"),
	INVALID_INGREDIENT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 재료 타입입니다.", "RECIPE-004"),
	INGREDIENTS_REQUIRED(HttpStatus.BAD_REQUEST, "재료 목록이 비어 있습니다.", "RECIPE-006"),
	SESSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 채택 완료된 세션입니다.", "RECIPE-007"),
	INVALID_DIFFICULTY(HttpStatus.BAD_REQUEST, "유효하지 않은 난이도입니다.", "RECIPE-008"),
	INGREDIENT_QUANTITY_INSUFFICIENT(HttpStatus.BAD_REQUEST, "재료 수량이 부족합니다.", "RECIPE-009"),
	INGREDIENT_UNIT_MISMATCH(HttpStatus.BAD_REQUEST, "재료 단위가 일치하지 않습니다.", "RECIPE-010"),
	INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "레시피 요청 메시지 타입 오류.", "RECIPE-012"),
	RECIPE_INGREDIENTS_REQUIRED(HttpStatus.BAD_REQUEST, "레시피 요청에 필요한 값이 누락되었습니다.", "RECIPE-013"),

	// 401
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.", "AUTH-001"),

	// 403 FORBIDDEN (권한 관련)
	NOT_MY_PLANT(HttpStatus.FORBIDDEN, "해당 식물에 대한 권한이 없습니다.", "PLANT-001"),
	AI_RECIPE_CHANGE_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "AI 레시피 생성은 최대 5회로 제한됩니다.", "RECIPE-002"),

	// 404 NOT FOUND
	NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.", "COMMON-003"),
	PLANT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 식물입니다.", "PLANT-002"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.", "USER-001"),
	INGREDIENT_REFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 식재료를 찾을 수 없습니다.", "INGREDIENT-006"),
	AI_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "sessionId를 찾을 수 없습니다.", "RECIPE-003"),
	INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "재료를 찾을 수 없습니다.", "RECIPE-005"),

	// 409
	DUPLICATE_CUSTOM_INGREDIENT(HttpStatus.CONFLICT, "이미 등록된 식재료 입니다.", "INGREDIENT-007"),

	//500
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 에러가 발생하였습니다.", "COMMON-004"),
	AI_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 호출에 실패하였습니다.", "RECIPE-001"),
	AI_RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 파싱에 실패했습니다.", "RECIPE-011"),
	AI_RESPONSE_INVALID_FORMAT(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 형식이 잘못되었습니다.", "RECIPE-012"),
	;

	private final HttpStatus httpStatus;
	private final String message;
	private final String code;
}