package com.cookeep.cookeep.common.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cookeep.cookeep.common.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.security.SignatureException;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ErrorResponse> handleAppCustomException(AppException e, HttpServletRequest request) {
		log.error("AppException 발생: {}", e.getErrorCode().getMessage());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
		ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode(), request);
		return ResponseEntity
			.status(e.getErrorCode().getHttpStatus())
			.body(errorResponse);
	}

	// JWT 토큰 관련 예외 처리
	// - JwtException: io.jsonwebtoken 라이브러리의 모든 JWT 예외
	// - AuthenticationException: Spring Security의 인증 예외
	@ExceptionHandler({
			JwtException.class,
			ExpiredJwtException.class,
			MalformedJwtException.class,
			SignatureException.class,
			UnsupportedJwtException.class,
			IllegalArgumentException.class
	})
	public ResponseEntity<ErrorResponse> handleAuthenticationException(
			Exception e, HttpServletRequest request) {
		log.error("인증 에러 발생: {}", e.getMessage());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

		ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED, request);
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(errorResponse);
	}

	// Validation 실패 (필수값 누락, @Valid 검증 실패)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
		MethodArgumentNotValidException e, HttpServletRequest request) {

		log.error("Validation 에러 발생: {}", e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

		Map<String, String> errors = new java.util.LinkedHashMap<>();

		// 필드 에러
		for (org.springframework.validation.FieldError fe : e.getBindingResult().getFieldErrors()) {
			String msg = fe.getDefaultMessage(); // DTO 내 에러 메세지
			if (msg == null || msg.isBlank()) msg = "요청 값이 올바르지 않습니다."; // 메세지가 비어있을 경우 기본 문구
			// key와 value로 Map에 저장
			// ex) SignupRequestDTO의 이메일 에러일 경우 key: "email", value: "이메일은 필수 입력 값입니다."
			// 여러 필드에서 에러가 동시에 발생할 수 있으므로 Map 형태로 저장
			// 동일 필드의 중복 에러는 첫 번째만 유지됨
			errors.putIfAbsent(fe.getField(), msg);
		}

		// 글로벌 에러 (@PasswordMatch 같은 타입레벨)
		for (org.springframework.validation.ObjectError oe : e.getBindingResult().getGlobalErrors()) {
			String msg = oe.getDefaultMessage();
			if (msg == null || msg.isBlank()) msg = "요청 값이 올바르지 않습니다.";
			errors.putIfAbsent("_global", msg); // key를 _global로 처리
		}

		ErrorResponse errorResponse = ErrorResponse.ofValidation(
			ErrorCode.INVALID_PARAMETER,
			errors,
			request
		);

		return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(errorResponse);
	}


	// JSON 파싱 실패 또는 ENUM 타입 불일치
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
			HttpMessageNotReadableException e, HttpServletRequest request) {
		log.error("JSON 파싱 에러 발생: {}", e.getMessage());
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

		// ENUM 타입 에러인지 확인
		String message = e.getMessage();
		ErrorCode errorCode;

		if (message != null && message.contains("Unit")) {
			errorCode = ErrorCode.INVALID_UNIT_TYPE;
		} else if (message != null && message.contains("Storage")) {
			errorCode = ErrorCode.INVALID_STORAGE_TYPE;
		} else {
			errorCode = ErrorCode.BAD_REQUEST;
		}

		ErrorResponse errorResponse = ErrorResponse.of(errorCode, request);
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(errorResponse);
	}

	// 처리되지 않은 모든 예외를 잡는 핸들러
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleAllException(Exception e, HttpServletRequest request) {
		log.error("처리되지 않은 예외 발생: ", e);
		log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
		ErrorResponse errorResponse = ErrorResponse.of(
				ErrorCode.INTERNAL_SERVER_ERROR,
				request
		);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(errorResponse);
	}
}