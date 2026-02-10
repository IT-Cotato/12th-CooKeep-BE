package com.cookeep.cookeep.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {
	@Value("${sms.api-key}") private String accountSid;
	@Value("${sms.api-secret}") private String authToken;

	// 서버 시작 시 Twilio SDK 초기화
	// Twilio.init는 전역 설정으로, 이후 sms 인증 시 사용됨
	@PostConstruct
	void init() {
		Twilio.init(accountSid, authToken);
	}
}