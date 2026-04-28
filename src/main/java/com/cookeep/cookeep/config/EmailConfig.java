package com.cookeep.cookeep.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class EmailConfig {

	@Value("${email.username}")
	private String username;
	@Value("${email.password}")
	private String password;

	@Bean
	public JavaMailSender emailSender() {

		JavaMailSenderImpl emailSender = new JavaMailSenderImpl(); // 구현체 생성
		emailSender.setHost("smtp.gmail.com"); // Gmail SMTP 서버
		emailSender.setPort(587); // TLS 방식
		emailSender.setUsername(username);
		emailSender.setPassword(password); // 앱 비밀번호

		// SMTP 동작 방식 세부 설정
		Properties javaMailProperties = new Properties();
		javaMailProperties.put("mail.transport.protocol", "smtp");
		javaMailProperties.put("mail.smtp.auth", "true");
		javaMailProperties.put("mail.smtp.starttls.enable", "true");
		javaMailProperties.put("mail.debug", "false");
		javaMailProperties.put("mail.smtp.ssl.trust", "smtp.gmail.com"); // gmail 인증서 신뢰
		javaMailProperties.put("mail.smtp.ssl.protocols", "TLSv1.3"); // 최신 TLS 사용

		emailSender.setJavaMailProperties(javaMailProperties);

		return emailSender;
	}
}