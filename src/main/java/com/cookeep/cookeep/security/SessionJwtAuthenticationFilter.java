package com.cookeep.cookeep.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cookeep.cookeep.common.dto.ErrorResponse;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.application.AuthSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SessionJwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final AuthSessionStore authSessionStore;
	private final ObjectMapper objectMapper;

	public SessionJwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		AuthSessionStore authSessionStore,
		ObjectMapper objectMapper
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.authSessionStore = authSessionStore;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));

		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			TokenClaims claims = jwtTokenProvider.parseAccessToken(token);
			if (authSessionStore.isActive(claims.userId(), claims.sessionId())) {
				UserPrincipal principal = new UserPrincipal(claims.userId());
				var authentication =
					new UsernamePasswordAuthenticationToken(principal, null, List.of());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (AppException e) {
			SecurityContextHolder.clearContext();
			if (e.getErrorCode() == ErrorCode.AUTH_SESSION_UNAVAILABLE) {
				writeSessionUnavailable(response, request);
				return;
			}
		} catch (RuntimeException e) {
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private void writeSessionUnavailable(
		HttpServletResponse response,
		HttpServletRequest request
	) throws IOException {
		response.setStatus(ErrorCode.AUTH_SESSION_UNAVAILABLE.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(
			response.getWriter(),
			ErrorResponse.of(ErrorCode.AUTH_SESSION_UNAVAILABLE, request)
		);
	}

	private String extractBearerToken(String header) {
		if (header == null || !header.startsWith("Bearer ")) {
			return null;
		}
		String token = header.substring(7).trim();
		return token.isEmpty() ? null : token;
	}
}
