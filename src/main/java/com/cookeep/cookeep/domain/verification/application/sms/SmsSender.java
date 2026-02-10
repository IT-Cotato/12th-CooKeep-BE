package com.cookeep.cookeep.domain.verification.application.sms;

// SMS 발송 추상화 인터페이스
// SMS 벤더 교체 가능성을 고려하여 구현체와 분리하였음
public interface SmsSender {
	void send(String to010, String text);
}
