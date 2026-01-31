package com.cookeep.cookeep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {
	// 비밀번호 암호화를 위한 BCryptPasswordEncoder Bean 등록 Config

	// 회원가입 시 비밀번호를 암호화하기 위해 사용
	// 로그인 시 matches()로 비밀번호 검증에 사용

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}