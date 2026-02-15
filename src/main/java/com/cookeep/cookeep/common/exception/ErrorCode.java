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
	NOT_ENOUGH_COOKIES(HttpStatus.BAD_REQUEST, "보유한 쿠키가 부족합니다.", "COOKIE-001"),
	INVALID_INGREDIENT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 재료 타입입니다.", "RECIPE-001"),
	INGREDIENTS_REQUIRED(HttpStatus.BAD_REQUEST, "재료 목록이 비어 있습니다.", "RECIPE-002"),
	SESSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 채택 완료된 세션입니다.", "RECIPE-03"),
	INVALID_DIFFICULTY(HttpStatus.BAD_REQUEST, "유효하지 않은 난이도입니다.", "RECIPE-004"),
	INGREDIENT_QUANTITY_INSUFFICIENT(HttpStatus.BAD_REQUEST, "재료 수량이 부족합니다.", "RECIPE-005"),
	INGREDIENT_UNIT_MISMATCH(HttpStatus.BAD_REQUEST, "재료 단위가 일치하지 않습니다.", "RECIPE-006"),
	INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "레시피 요청 메시지 타입 오류.", "RECIPE-007"),
	RECIPE_INGREDIENTS_REQUIRED(HttpStatus.BAD_REQUEST, "레시피 요청에 필요한 값이 누락되었습니다.", "RECIPE-008"),
	AI_RECIPE_CHANGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "레시피 요청은 최대 5번입니다.", "RECIPE-009"),
	INVALID_FOOD_TYPE_COUNT(HttpStatus.BAD_REQUEST, "선호하는 음식 종류는 3개까지만 선택 가능합니다.", "ONBOARDING-001"),
	INVALID_WEEKLY_GOAL_TARGET_COUNT(HttpStatus.BAD_REQUEST, "주간 목표가 설정된 경우 목표 횟수를 필수로 입력해야 합니다.", "ONBOARDING-002"),
	INVALID_TARGET_COUNT(HttpStatus.BAD_REQUEST, "목표 횟수는 1에서 10 사이의 정수여야 합니다.", "ONBOARDING-003"),
	RECIPE_SESSIONID_REQUIRED(HttpStatus.BAD_REQUEST, "레시피 요청에 필요한 값이 누락되었습니다.", "RECIPE-016"),
	SESSION_DIFFICULTY_NOT_FOUND(HttpStatus.BAD_REQUEST, "세션의 난이도 정보를 찾을 수 없습니다.", "RECIPE-017"),
	SESSION_INGREDIENTS_NOT_FOUND(HttpStatus.BAD_REQUEST, "세션의 재료 정보를 찾을 수 없습니다.", "RECIPE-018"),
	REFRIGERATOR_INVALID_QUERY(HttpStatus.BAD_REQUEST, "잘못된 쿼리 파라미터입니다.", "REFRIGERATOR-001"),
	INVALID_SORT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 정렬 타입입니다.", "REFRIGERATOR-002"),
	REFRIGERATOR_SEARCH_QUERY_REQUIRED(HttpStatus.BAD_REQUEST,"검색어를 입력해주세요.", "REFRIGERATOR-003"),
	INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 전화번호 형식입니다.", "SMS-001"),
	INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.", "SMS-002"),
	VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다.", "SMS-003"),
	// VERIFICATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "전화번호 인증을 실패했습니다.", "SMS-004"),
	INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "식재료 수량은 0 이상이어야 합니다.", "INGREDIENT_008"),
	MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "식재료 메모는 최대 100자까지 입력 가능합니다.", "INGREDIENT_009"),
	INVALID_DELETE_REQUEST(HttpStatus.BAD_REQUEST, "삭제할 식재료를 입력해주세요.", "INGREDIENT_010"),
	DAILY_RECIPE_UPDATE_FIELDS_REQUIRED(HttpStatus.BAD_REQUEST, "수정할 항목(제목 또는 한줄평)을 입력해주세요.", "DAILY_RECIPE-004"),
	SAME_AS_PREVIOUS_PASSWORD(HttpStatus.BAD_REQUEST, "기존 등록된 비밀번호와 동일한 비밀번호입니다.", "AUTH-005"),
	SAME_AS_CURRENT_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "기존 등록된 전화번호와 동일한 전화번호입니다.", "USER-007"),
	TITLE_INVALID_VALUE(HttpStatus.BAD_REQUEST, "레시피 제목을 입력해주세요.", "RECIPE-022"),
	SAME_AS_CURRENT_EMAIL(HttpStatus.BAD_REQUEST, "기존 등록된 이메일과 동일한 이메일입니다.", "USER-008"),
	TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "레시피 제목은 최대 100글자입니다.", "RECIPE-023"),
	CANNOT_LIKE_OWN_RECIPE(HttpStatus.BAD_REQUEST, "자신의 레시피에는 좋아요를 누를 수 없습니다.", "DAILY_RECIPE-006"),
	CANNOT_BOOKMARK_OWN_RECIPE(HttpStatus.BAD_REQUEST, "자신의 레시피에는 북마크를 누를 수 없습니다.", "DAILY_RECIPE-007"),
	VERIFICATION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "전화번호 인증이 완료되지 않았습니다.", "SMS-009"),
	PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.", "AUTH-006"),
	REGISTERED_PHONE_NUMBER_MISMATCH(HttpStatus.BAD_REQUEST, "회원정보에 등록된 전화번호와 일치하지 않습니다.", "AUTH-008"),
	RECIPE_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "데일리레시피에 기록된 레시피는 삭제할 수 없습니다.", "RECIPE-024"),

	// 401 UNAUTHORIZED (인증 관련)
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.", "AUTH-001"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레쉬 토큰입니다.", "AUTH-002"),
	AUTH_PASSWORD_MISMATCH (HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.", "AUTH-003"),

	// 403 FORBIDDEN (권한 관련)
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", "COMMON-003"),
	NOT_MY_PLANT(HttpStatus.FORBIDDEN, "해당 식물에 대한 권한이 없습니다.", "PLANT-001"),
	AI_SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 대화 세션이 아닙니다.", "RECIPE-015"),
	DAILY_RECIPE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 레시피가 아닙니다.", "DAILY_RECIPE-001"),
	SOCIAL_USER_EMAIL_CHANGE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "소셜 로그인 사용자는 이메일을 변경할 수 없습니다.", "USER-009"),

	// 404 NOT FOUND
	NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.", "COMMON-004"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.", "COMMON-005"),
	PLANT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 식물입니다.", "PLANT-002"),
	INGREDIENT_REFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 식재료를 찾을 수 없습니다.", "INGREDIENT-006"),
	AI_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 AI 세션을 찾을 수 없습니다.", "RECIPE-010"),
	INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 재료입니다.", "RECIPE-011"),
	AUTH_PHONE_NOT_REGISTERED (HttpStatus.NOT_FOUND, "가입되지 않은 전화번호입니다.", "AUTH-004"),
	AI_RECIPE_TITLE_MISSING(HttpStatus.NOT_FOUND, "AI 응답에 레시피 제목이 존재하지 않습니다.", "RECIPE-019"),
	AI_RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 AI 레시피를 찾을 수 없습니다.", "DAILY_RECIPE-002"),
	DAILY_RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 데일리 레시피를 찾을 수 없습니다.", "DAILY_RECIPE-003"),
	VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 요청 내역이 없습니다.", "SMS-004"),

	// 409
	DUPLICATE_CUSTOM_INGREDIENT(HttpStatus.CONFLICT, "이미 등록된 식재료 입니다.", "INGREDIENT-007"),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.", "USER-001"),
	USER_PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다.", "USER-002"),
	USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.", "USER-003"),
	USER_EMAIL_REGISTERED_WITH_KAKAO(HttpStatus.CONFLICT,"이미 카카오로 가입된 이메일입니다.", "USER-004"),
	USER_EMAIL_REGISTERED_WITH_GOOGLE(HttpStatus.CONFLICT,"이미 구글로 가입된 이메일입니다.", "USER-005"),
	USER_EMAIL_REGISTERED_WITH_KAKAO_GOOGLE(HttpStatus.CONFLICT,"이미 카카오와 구글로 가입된 이메일입니다.", "USER-006"),
	WEEKLY_GOAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이번 주 목표가 이미 설정되어 있습니다.", "WEEKLY_GOAL-001"),
	DAILY_RECIPE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 데일리 레시피로 등록된 AI 레시피입니다.", "DAILY_RECIPE-005"),

	// 423
	PASSWORD_VERIFICATION_LOCKED(HttpStatus.LOCKED, "비밀번호 입력 횟수를 초과했습니다. 본인인증 후 다시 시도해주세요.", "AUTH-007"),

	// 429
	SMS_RESEND_TOO_FAST(HttpStatus.TOO_MANY_REQUESTS, "인증번호 재전송 요청이 너무 빠릅니다. 잠시 후 다시 시도해주세요.", "SMS-005"),
	SMS_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과하였습니다. 잠시 후 다시 시도해주세요.", "SMS-006"),

	//500
	FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.", "FILE-001"),
	AI_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 요청 또는 저장 처리에 실패했습니다.","RECIPE-012"),
	AI_RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 파싱에 실패했습니다.","RECIPE-013"),
	AI_RESPONSE_INVALID_FORMAT(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 형식이 올바르지 않습니다.","RECIPE-014"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 에러가 발생하였습니다.", "COMMON-006"),
	RECIPE_TITLE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"레시피 제목 파싱 실패","RECIPE-020"),
	INGREDIENTS_JSON_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"재료 JSON 변환에 실패했습니다.","RECIPE-021"),
	SMS_PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 서비스 오류가 발생했습니다.", "SMS-007"),
	SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "인증번호 발송 중 오류가 발생했습니다.", "SMS-008"),
	USERAUTH_DOES_NOT_EXIST (HttpStatus.INTERNAL_SERVER_ERROR, "UserAuth 정보가 존재하지 않습니다.", "AUTH-003"),


	// 503 SERVICE_UNAVAILABLE
	NICKNAME_GENERATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"닉네임을 생성할 수 없습니다. 잠시 후 다시 시도해주세요.", "NICKNAME-001"),
	;

	private final HttpStatus httpStatus;
	private final String message;
	private final String code;
}
