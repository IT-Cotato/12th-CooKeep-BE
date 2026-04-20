package com.cookeep.cookeep.domain.verification.application.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("emailVerificationSender")
@RequiredArgsConstructor
public class EmailVerificationSender implements VerificationSender {

	private final JavaMailSender mailSender;

	// 발신자 메일
	@Value("${email.username}")
	private String from;

	@Override
	public void send(String to, String title, String content) {
		try {
			log.info("[EMAIL] send attempt. to={}", to);
			// 실제 이메일 객체 생성
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");

			helper.setFrom(from, "COOKEEP");
			helper.setTo(to);
			helper.setSubject(title);
			helper.setText(content, true);

			mailSender.send(message);
			log.info("[EMAIL] send success. to={}", to);
		} catch (Exception e) {
			log.error("[EMAIL] CoolSMS send failed. to={}", to, e);
			throw new AppException(ErrorCode.EMAIL_PROVIDER_ERROR);
		}
	}
}