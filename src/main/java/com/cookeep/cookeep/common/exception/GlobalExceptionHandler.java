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

		ErrorResponse errorResponse = ErrorResponse.of(
				ErrorCode.INGREDIENT_REQUIRED_FIELDS_MISSING,
				request
		);
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(errorResponse);
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