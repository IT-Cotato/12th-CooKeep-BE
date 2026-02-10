package com.cookeep.cookeep.domain.verification.application.sms;

// SMS 발송 추상화 인터페이스
// 현재는 Twilio로 구현하였으나 CoolSMS 등 다른 SMS 벤더로 교체될 가능성을 고려하여 구현체와 분리하였음
public interface SmsSender {
	// Twilio는 국제 서비스이므로 핸드폰 번호 국제 표준인 E.164 형식으로 발송
	void send(String toE164, String text);
}
