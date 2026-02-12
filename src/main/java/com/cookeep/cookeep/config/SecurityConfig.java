package com.cookeep.cookeep.config;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cookeep.cookeep.security.CustomAccessDeniedHandler;
import com.cookeep.cookeep.security.CustomAuthenticationEntryPoint;
import com.cookeep.cookeep.security.JwtAuthenticationFilter;
import com.cookeep.cookeep.security.JwtTokenProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(
			"http://localhost:5173",
			"https://12th-coo-keep-fe.vercel.app",
			"https://cookeep.store",
			"https://api.cookeep.store"
		));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 방식이므로 로그인 상태 저장 X
			.formLogin(form -> form.disable()) // 별도의 로그인 api 존재하므로 스프링 시큐리티 기본 로그인 페이지는 비활성화하였음
			.httpBasic(basic -> basic.disable())

			.exceptionHandling(eh -> eh
				.authenticationEntryPoint(customAuthenticationEntryPoint) // 401 에러 처리
				.accessDeniedHandler(customAccessDeniedHandler)           // 403 에러 처리
			)

			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					// 아래 경로들은 로그인하지 않아도 접근 가능
					// 나머지 경로는 모두 로그인해야 접근 가능함
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/swagger-resources/**",
					"/error"
				).permitAll()

				// auth 경로 중 로그아웃은 로그인한 사용자만 가능하도록 처리
				.requestMatchers("/api/auth/logout").authenticated()

				// 나머지 auth 경로는 로그인하지 않아도 접근 가능
				.requestMatchers("/api/auth/**").permitAll()

				.anyRequest().authenticated()
			)

			// JWT 필터 먼저 실행
			.addFilterBefore(
				new JwtAuthenticationFilter(jwtTokenProvider),
				UsernamePasswordAuthenticationFilter.class
			);

		return http.build();
	}
}
