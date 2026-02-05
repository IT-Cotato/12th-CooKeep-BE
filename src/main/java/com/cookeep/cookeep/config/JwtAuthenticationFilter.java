package com.cookeep.cookeep.config;

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	// JwtTokenProvider : 토큰 생성/검증/파싱
	// JwtAuthenticationFilter : 요청에서 토큰을 추출하여 JwtTokenProvider로 검증, SecurityContext에 인증 정보 설정
	// SecurityConfig : Spring Security 전반 설정

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		// 요청 헤더의 Authorization 값을 가져옴
		// "Bearer " 검사 후 토큰 추출, 잘못된 형식일 경우 null을 반환함
		String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));

		try {
			if (token != null && jwtTokenProvider.validateToken(token, false)) {
				// 액세스 토큰이 존재하고 유효할 경우 userId를 추출
				Long userId = jwtTokenProvider.getUserId(token, false);

				UserPrincipal principal = new UserPrincipal(userId);

				var authentication =
					new UsernamePasswordAuthenticationToken(principal, null, List.of());

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (JwtException | IllegalArgumentException e) {
			SecurityContextHolder.clearContext();
		}

		log.debug("Authorization header = {}", request.getHeader("Authorization"));

		filterChain.doFilter(request, response);
	}

	private String extractBearerToken(String header) {
		if (header == null) return null; // Authorization 헤더 없는 요청일 경우 null 반환
		if (!header.startsWith("Bearer ")) return null; // "Bearer " 형식 아닐 경우 null 반환
		String token = header.substring(7).trim();
		return token.isEmpty() ? null : token;
	}
}