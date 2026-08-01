package com.cookeep.cookeep.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.application.AuthSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SessionJwtAuthenticationFilterTest {

	@Mock private JwtTokenProvider jwtTokenProvider;
	@Mock private AuthSessionStore authSessionStore;

	private SessionJwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		filter = new SessionJwtAuthenticationFilter(
			jwtTokenProvider,
			authSessionStore,
			new ObjectMapper().findAndRegisterModules()
		);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void activeSessionAuthenticatesAccessToken() throws Exception {
		TokenClaims claims = new TokenClaims(1L, "session-id", Instant.now().plusSeconds(60));
		given(jwtTokenProvider.parseAccessToken("access-token")).willReturn(claims);
		given(authSessionStore.isActive(1L, "session-id")).willReturn(true);

		MockHttpServletRequest request = requestWithToken("access-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();
		assertThat(principal.userId()).isEqualTo(1L);
	}

	@Test
	void revokedSessionDoesNotAuthenticateAccessToken() throws Exception {
		TokenClaims claims = new TokenClaims(1L, "session-id", Instant.now().plusSeconds(60));
		given(jwtTokenProvider.parseAccessToken("access-token")).willReturn(claims);
		given(authSessionStore.isActive(1L, "session-id")).willReturn(false);

		filter.doFilter(
			requestWithToken("access-token"),
			new MockHttpServletResponse(),
			new MockFilterChain()
		);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void redisFailureReturnsServiceUnavailableWithoutContinuingChain() throws Exception {
		TokenClaims claims = new TokenClaims(1L, "session-id", Instant.now().plusSeconds(60));
		given(jwtTokenProvider.parseAccessToken("access-token")).willReturn(claims);
		given(authSessionStore.isActive(1L, "session-id"))
			.willThrow(new AppException(ErrorCode.AUTH_SESSION_UNAVAILABLE));

		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(
			requestWithToken("access-token"),
			response,
			new MockFilterChain()
		);

		assertThat(response.getStatus()).isEqualTo(503);
		assertThat(response.getContentAsString()).contains("AUTH-016");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	private MockHttpServletRequest requestWithToken(String token) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		return request;
	}
}
