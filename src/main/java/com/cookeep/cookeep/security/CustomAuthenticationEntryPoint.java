package com.cookeep.cookeep.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.cookeep.cookeep.common.dto.ErrorResponse;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
	// 인증되지 않은 사용자가 보호된 리소스에 접근할 때 401 응답을 반환하는 핸들러

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

		ErrorResponse body = ErrorResponse.of(errorCode, request);

		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(
			objectMapper.copy()
				.registerModule(new JavaTimeModule())
				.writeValueAsString(body)
		);
	}
}
