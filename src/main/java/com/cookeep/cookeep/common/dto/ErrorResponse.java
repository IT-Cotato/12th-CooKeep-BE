package com.cookeep.cookeep.common.dto;

import java.util.Map;

import org.springframework.http.HttpStatus;

import com.cookeep.cookeep.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

@Getter
public class ErrorResponse extends BaseResponse {

	private final String code;
	private final String message;
	private final String method;
	private final String requestURI;

	private final Map<String, String> errors;

	private ErrorResponse(String code, String message, String method, String requestURI,
		Map<String, String> errors, HttpStatus httpStatus) {
		super(httpStatus);
		this.code = code;
		this.message = message;
		this.method = method;
		this.requestURI = requestURI;
		this.errors = errors;
	}

	public static ErrorResponse of(ErrorCode errorCode, HttpServletRequest request) {
		return new ErrorResponse(
			errorCode.getCode(),
			errorCode.getMessage(),
			request.getMethod(),
			request.getRequestURI(),
			null, // errors가 없는 경우
			errorCode.getHttpStatus()
		);
	}

	// Validation 예외 전용 ErrorResponse 생성 메서드 (field errors 포함)
	// 여러 필드에서 에러가 동시에 발생할 수 있으므로 Map 형태로 저장
	public static ErrorResponse ofValidation(ErrorCode errorCode, Map<String, String> errors, HttpServletRequest request) {
		return new ErrorResponse(
			errorCode.getCode(),
			errorCode.getMessage(),
			request.getMethod(),
			request.getRequestURI(),
			errors, // Validation 실패 시 필드별 오류 메시지 (key: 필드명, value: 에러 메시지)
			errorCode.getHttpStatus()
		);
	}

	// 추가 에러 정보(시도 횟수 등)를 포함한 ErrorResponse 생성 메서드
	public static ErrorResponse ofWithErrors(ErrorCode errorCode, Map<String, String> errors, HttpServletRequest request) {
		return new ErrorResponse(
			errorCode.getCode(),
			errorCode.getMessage(),
			request.getMethod(),
			request.getRequestURI(),
			errors,
			errorCode.getHttpStatus()
		);
	}

}